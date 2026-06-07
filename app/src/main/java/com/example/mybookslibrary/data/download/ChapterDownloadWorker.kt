@file:Suppress(
    "ForbiddenComment",
    "ktlint:standard:function-signature",
    "ktlint:standard:indent",
    "ktlint:standard:max-line-length",
)

package com.example.mybookslibrary.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.remote.AtHomeReportPolicy
import com.example.mybookslibrary.data.remote.models.AtHomeReportRequest
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
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Named
import kotlin.time.TimeSource

/**
 * WorkManager worker that downloads all pages for a chapter into app-private storage.
 */
// TODO: Split and reformat the legacy chapter download workflow, then remove these ktlint suppressions.
@HiltWorker
class ChapterDownloadWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParameters: WorkerParameters,
        private val mangaRepository: MangaRepository,
        private val offlineDownloadRepository: OfflineDownloadRepository,
        private val offlineDownloadStorage: OfflineDownloadStorage,
        private val downloadNotifier: DownloadNotifier,
        @param:Named("ImageOkHttpClient") private val imageOkHttpClient: OkHttpClient,
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
            setForeground(downloadNotifier.createForegroundInfo(chapterId, progressPercent = 0, indeterminate = true))
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
                        errorThreshold = FAILOVER_ERROR_THRESHOLD,
                )
                val completedPages = AtomicInteger(0)
                setForeground(downloadNotifier.createForegroundInfo(chapterId, progressPercent = 0, indeterminate = false))

                (0 until failoverCoordinator.totalPages)
                    .asFlow()
                    .flatMapMerge(concurrency = PAGE_DOWNLOAD_CONCURRENCY) { pageIndex ->
                        flow {
                            currentCoroutineContext().ensureActive()
                            downloadPageWithFailover(
                                mangaId = mangaId,
                                chapterId = chapterId,
                                pageIndex = pageIndex,
                                failoverCoordinator = failoverCoordinator,
                            )
                            val completed = completedPages.incrementAndGet()
                            val totalPages = failoverCoordinator.totalPages
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
                            setForeground(downloadNotifier.createForegroundInfo(chapterId, progressPercent = progress, indeterminate = false))
                            emit(Unit)
                        }
                    }.collect()

                offlineDownloadStorage.markChapterComplete(
                    mangaId = mangaId,
                    chapterId = chapterId,
                )
                // Marker filesystem là nguồn sự thật (đã tải xong). Cập nhật DB/cache là dẫn xuất;
                // nếu lỗi, scan lúc khởi động dựng lại từ marker → KHÔNG coi là tải thất bại.
                try {
                    offlineDownloadRepository.markChapterDownloaded(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        totalPages = failoverCoordinator.totalPages,
                    )
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (e: Exception) {
                    Timber.w(e, "markChapterDownloaded lỗi sau khi đã ghi marker (scan sẽ tự sửa): chapterId=%s", chapterId)
                }
                setProgress(workDataOf(KEY_PROGRESS_PERCENT to 100))
                setForeground(downloadNotifier.createForegroundInfo(chapterId, progressPercent = 100, indeterminate = false))
                downloadNotifier.showFinishedNotification(chapterId, success = true, message = "Chapter download complete")
                Timber.d(
                    "ChapterDownloadWorker success: mangaId=%s chapterId=%s pages=%d",
                    mangaId,
                    chapterId,
                    failoverCoordinator.totalPages,
                )
                Result.success()
            } catch (cancellationException: CancellationException) {
                // Cancel (vd WorkManager hủy work) phải propagate sạch, không ghi thành ERROR
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

        /**
         * Downloads one page and retries it after coordinated MangaDex@Home failover.
         *
         * The worker downloads pages concurrently. When three page downloads fail in
         * completion order, [AtHomeFailoverCoordinator] refreshes
         * `/at-home/server/{chapterId}` once and pauses URL construction while the
         * new server metadata is being fetched.
         */
        private suspend fun downloadPageWithFailover(
            mangaId: String,
            chapterId: String,
            pageIndex: Int,
            failoverCoordinator: AtHomeFailoverCoordinator,
        ) {
            var attempt = 1
            while (true) {
                currentCoroutineContext().ensureActive()
                val pageUrl = failoverCoordinator.pageUrl(pageIndex)

                try {
                    downloadPage(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        pageIndex = pageIndex,
                        pageUrl = pageUrl,
                    )
                    failoverCoordinator.onPageSuccess()
                    return
                } catch (t: Throwable) {
                    currentCoroutineContext().ensureActive()
                    val failoverTriggered = failoverCoordinator.onPageFailure(chapterId)
                    Timber.e(
                        t,
                        "downloadPage attempt failed: chapterId=%s pageIndex=%d attempt=%d failoverTriggered=%s",
                        chapterId,
                        pageIndex,
                        attempt,
                        failoverTriggered,
                    )

                    if (attempt >= MAX_PAGE_DOWNLOAD_ATTEMPTS) {
                        Timber.e(
                            t,
                            "downloadPage exhausted attempts: chapterId=%s pageIndex=%d attempts=%d",
                            chapterId,
                            pageIndex,
                            attempt,
                        )
                        throw t
                    }

                    attempt += 1
                    Timber.d(
                        "downloadPage retry scheduled: chapterId=%s pageIndex=%d nextAttempt=%d",
                        chapterId,
                        pageIndex,
                        attempt,
                    )
                }
            }
        }

        private suspend fun downloadPage(
            mangaId: String,
            chapterId: String,
            pageIndex: Int,
            pageUrl: String,
        ) {
            Timber.d("downloadPage start: chapterId=%s pageIndex=%d url=%s", chapterId, pageIndex, pageUrl)
            val startedAt = TimeSource.Monotonic.markNow()
            var bytes = 0L
            var cached = false
            var success = false

            try {
                val request =
                    Request
                        .Builder()
                        .url(pageUrl)
                        .header(AtHomeReportPolicy.SKIP_REPORT_HEADER, "true")
                        .build()
                imageOkHttpClient.newCall(request).execute().use { response ->
                    cached = response
                        .header(HEADER_X_CACHE)
                        ?.startsWith(CACHE_HIT_PREFIX, ignoreCase = true) == true
                    val body = response.body
                    if (!response.isSuccessful) {
                        bytes = responseBodySize(body)
                        throw IllegalStateException("Page download failed: HTTP ${response.code}")
                    }
                    val savedFile =
                        offlineDownloadStorage.savePage(
                            mangaId = mangaId,
                            chapterId = chapterId,
                            pageIndex = pageIndex,
                            byteStream = body.byteStream(),
                            extension = extensionFor(pageUrl, body.contentType()?.subtype),
                        )
                    bytes = savedFile.length()
                    success = true
                }
            } catch (ioException: IOException) {
                Timber.e(ioException, "downloadPage network error: chapterId=%s pageIndex=%d", chapterId, pageIndex)
                throw ioException
            } finally {
                val durationMillis = startedAt.elapsedNow().inWholeMilliseconds
                sendDownloadReport(
                    pageUrl = pageUrl,
                    success = success,
                    bytes = bytes,
                    durationMillis = durationMillis,
                    cached = cached,
                )
            }
            Timber.d("downloadPage end: chapterId=%s pageIndex=%d bytes=%d", chapterId, pageIndex, bytes)
        }

        private suspend fun sendDownloadReport(
            pageUrl: String,
            success: Boolean,
            bytes: Long,
            durationMillis: Long,
            cached: Boolean,
        ) {
            if (!AtHomeReportPolicy.isReportableImageUrl(pageUrl)) {
                Timber.d("downloadPage report skipped: url=%s", pageUrl)
                return
            }

            val report =
                AtHomeReportRequest(
                    url = pageUrl,
                    success = success,
                    bytes = AtHomeReportPolicy.bytesToInt(bytes),
                    duration = durationMillis.coerceAtLeast(0L),
                    cached = cached,
                )
            Timber.d("downloadPage report: payload=%s", report)
            mangaRepository.sendAtHomeReport(report)
        }

        private fun responseBodySize(body: okhttp3.ResponseBody): Long =
            try {
                body.bytes().size.toLong()
            } catch (t: Throwable) {
                Timber.e(t, "responseBodySize failed")
                0L
            }

        private fun extensionFor(
            pageUrl: String,
            contentSubtype: String?,
        ): String {
            val subtype = contentSubtype?.lowercase()
            return when {
                subtype == "jpeg" || subtype == "jpg" -> "jpg"
                subtype == "png" -> "png"
                subtype == "webp" -> "webp"
                subtype == "gif" -> "gif"
                else ->
                    pageUrl
                        .substringBefore("?")
                        .substringAfterLast(".", missingDelimiterValue = "img")
                        .takeIf { it.length in 2..5 }
                        ?: "img"
            }
        }

        companion object {
            const val KEY_MANGA_ID = "manga_id"
            const val KEY_CHAPTER_ID = "chapter_id"
            const val KEY_PROGRESS_PERCENT = "progress_percent"
            const val KEY_ERROR = "error"

            private const val PAGE_DOWNLOAD_CONCURRENCY = 3
            private const val FAILOVER_ERROR_THRESHOLD = 3
            private const val MAX_PAGE_DOWNLOAD_ATTEMPTS = 3
            private const val HEADER_X_CACHE = "X-Cache"
            private const val CACHE_HIT_PREFIX = "HIT"
        }
    }
