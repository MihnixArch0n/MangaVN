package com.example.mybookslibrary.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * WorkManager worker that downloads all pages for a chapter into app-private storage.
 */
@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val mangaRepository: MangaRepository,
    private val offlineDownloadRepository: OfflineDownloadRepository,
    private val offlineDownloadStorage: OfflineDownloadStorage,
    private val downloadNotifier: DownloadNotifier,
    private val pageDownloader: PageDownloader,
) : CoroutineWorker(appContext, workerParameters) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun doWork(): Result {
        val mangaId = inputData.getString(KEY_MANGA_ID).orEmpty()
        val chapterId = inputData.getString(KEY_CHAPTER_ID).orEmpty()

        if (mangaId.isBlank() || chapterId.isBlank()) {
            Timber.e("ChapterDownloadWorker missing input: mangaId=%s chapterId=%s", mangaId, chapterId)
            return Result.failure(workDataOf(KEY_ERROR to "Missing mangaId or chapterId"))
        }

        Timber.d("ChapterDownloadWorker start: mangaId=%s chapterId=%s", mangaId, chapterId)
        setForeground(
            downloadNotifier.createForegroundInfo(
                chapterId = chapterId,
                progressPercent = 0,
                indeterminate = true,
            ),
        )
        offlineDownloadRepository.updateQueueStatus(chapterId, DownloadStatus.DOWNLOADING, 0)

        return try {
            val chapterDelivery = mangaRepository.getChapterDelivery(chapterId).getOrThrow()
            if (chapterDelivery.filenames.isEmpty()) {
                throw IllegalStateException("Chapter has no pages")
            }

            val failoverCoordinator =
                AtHomeFailoverCoordinator(
                    initialDelivery = chapterDelivery,
                    refreshDelivery = { mangaRepository.getChapterDelivery(chapterId).getOrThrow() },
                )
            val completedPages = AtomicInteger(0)
            setForeground(
                downloadNotifier.createForegroundInfo(
                    chapterId = chapterId,
                    progressPercent = 0,
                    indeterminate = false,
                ),
            )

            (0 until failoverCoordinator.totalPages)
                .asFlow()
                .flatMapMerge(concurrency = PAGE_DOWNLOAD_CONCURRENCY) { pageIndex ->
                    flow {
                        currentCoroutineContext().ensureActive()
                        pageDownloader.downloadPageWithFailover(
                            mangaId = mangaId,
                            chapterId = chapterId,
                            pageIndex = pageIndex,
                            failoverCoordinator = failoverCoordinator,
                        )
                        updateDownloadProgress(
                            chapterId = chapterId,
                            completed = completedPages.incrementAndGet(),
                            totalPages = failoverCoordinator.totalPages,
                        )
                        emit(Unit)
                    }
                }.collect()

            offlineDownloadStorage.markChapterComplete(
                mangaId = mangaId,
                chapterId = chapterId,
            )
            // Marker filesystem là nguồn sự thật (đã tải xong). Cập nhật DB/cache là dẫn xuất;
            // nếu lỗi, scan lúc khởi động dựng lại từ marker -> KHÔNG coi là tải thất bại.
            try {
                offlineDownloadRepository.markChapterDownloaded(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    totalPages = failoverCoordinator.totalPages,
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (e: Exception) {
                Timber.w(
                    e,
                    "markChapterDownloaded lỗi sau khi đã ghi marker (scan sẽ tự sửa): chapterId=%s",
                    chapterId,
                )
            }
            setProgress(workDataOf(KEY_PROGRESS_PERCENT to 100))
            setForeground(
                downloadNotifier.createForegroundInfo(
                    chapterId = chapterId,
                    progressPercent = 100,
                    indeterminate = false,
                ),
            )
            downloadNotifier.showFinishedNotification(
                chapterId = chapterId,
                success = true,
                message = "Chapter download complete",
            )
            Timber.d(
                "ChapterDownloadWorker success: mangaId=%s chapterId=%s pages=%d",
                mangaId,
                chapterId,
                failoverCoordinator.totalPages,
            )
            Result.success()
        } catch (cancellationException: CancellationException) {
            // Cancel (vd WorkManager hủy work) phải propagate sạch, không ghi thành ERROR.
            throw cancellationException
        } catch (t: Throwable) {
            Timber.e(t, "ChapterDownloadWorker failed: mangaId=%s chapterId=%s", mangaId, chapterId)
            offlineDownloadRepository.updateQueueStatus(
                chapterId = chapterId,
                status = DownloadStatus.ERROR,
                progressPercent = 0,
                errorMessage = t.message,
            )
            downloadNotifier.showFinishedNotification(
                chapterId = chapterId,
                success = false,
                message = t.message ?: "Chapter download failed",
            )
            Result.failure(workDataOf(KEY_ERROR to (t.message ?: "Download failed")))
        }
    }

    private suspend fun updateDownloadProgress(
        chapterId: String,
        completed: Int,
        totalPages: Int,
    ) {
        val progress = ((completed * 100f) / totalPages).toInt().coerceIn(0, 100)
        Timber.d(
            "ChapterDownloadWorker progress: chapterId=%s completed=%d total=%d progress=%d",
            chapterId,
            completed,
            totalPages,
            progress,
        )
        offlineDownloadRepository.updateQueueStatus(
            chapterId = chapterId,
            status = DownloadStatus.DOWNLOADING,
            progressPercent = progress,
        )
        setProgress(workDataOf(KEY_PROGRESS_PERCENT to progress))
        setForeground(
            downloadNotifier.createForegroundInfo(
                chapterId = chapterId,
                progressPercent = progress,
                indeterminate = false,
            ),
        )
    }

    companion object {
        const val KEY_MANGA_ID = "manga_id"
        const val KEY_CHAPTER_ID = "chapter_id"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_ERROR = "error"

        internal const val PAGE_DOWNLOAD_CONCURRENCY = 3
    }
}
