package com.example.mybookslibrary.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Named
import kotlin.math.absoluteValue

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
    @param:Named("ImageOkHttpClient") private val imageOkHttpClient: OkHttpClient
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
        setForeground(createForegroundInfo(chapterId, progressPercent = 0, indeterminate = true))
        offlineDownloadRepository.updateQueueStatus(chapterId, DownloadStatus.DOWNLOADING, 0)

        return try {
            val pageUrls = mangaRepository.getChapterPages(chapterId).getOrThrow()
            if (pageUrls.isEmpty()) {
                throw IllegalStateException("Chapter has no pages")
            }

            val completedPages = AtomicInteger(0)
            setForeground(createForegroundInfo(chapterId, progressPercent = 0, indeterminate = false))

            pageUrls.withIndex()
                .asFlow()
                .flatMapMerge(concurrency = PAGE_DOWNLOAD_CONCURRENCY) { indexedPage ->
                    flow {
                        currentCoroutineContext().ensureActive()
                        downloadPage(
                            mangaId = mangaId,
                            chapterId = chapterId,
                            pageIndex = indexedPage.index,
                            pageUrl = indexedPage.value
                        )
                        val completed = completedPages.incrementAndGet()
                        val progress = ((completed * 100f) / pageUrls.size).toInt().coerceIn(0, 100)
                        Timber.d(
                            "ChapterDownloadWorker progress: chapterId=%s completed=%d total=%d progress=%d",
                            chapterId,
                            completed,
                            pageUrls.size,
                            progress
                        )
                        offlineDownloadRepository.updateQueueStatus(
                            chapterId = chapterId,
                            status = DownloadStatus.DOWNLOADING,
                            progressPercent = progress
                        )
                        setProgress(workDataOf(KEY_PROGRESS_PERCENT to progress))
                        setForeground(createForegroundInfo(chapterId, progressPercent = progress, indeterminate = false))
                        emit(Unit)
                    }
                }
                .collect()

            offlineDownloadRepository.markChapterDownloaded(
                mangaId = mangaId,
                chapterId = chapterId,
                totalPages = pageUrls.size
            )
            setProgress(workDataOf(KEY_PROGRESS_PERCENT to 100))
            setForeground(createForegroundInfo(chapterId, progressPercent = 100, indeterminate = false))
            Timber.d("ChapterDownloadWorker success: mangaId=%s chapterId=%s pages=%d", mangaId, chapterId, pageUrls.size)
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "ChapterDownloadWorker failed: mangaId=%s chapterId=%s", mangaId, chapterId)
            offlineDownloadRepository.updateQueueStatus(
                chapterId = chapterId,
                status = DownloadStatus.ERROR,
                progressPercent = 0,
                errorMessage = t.message
            )
            Result.failure(workDataOf(KEY_ERROR to (t.message ?: "Download failed")))
        }
    }

    private suspend fun downloadPage(
        mangaId: String,
        chapterId: String,
        pageIndex: Int,
        pageUrl: String
    ) {
        Timber.d("downloadPage start: chapterId=%s pageIndex=%d url=%s", chapterId, pageIndex, pageUrl)
        val request = Request.Builder().url(pageUrl).build()
        imageOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Page download failed: HTTP ${response.code}")
            }
            val body = response.body
            offlineDownloadStorage.savePage(
                mangaId = mangaId,
                chapterId = chapterId,
                pageIndex = pageIndex,
                byteStream = body.byteStream(),
                extension = extensionFor(pageUrl, body.contentType()?.subtype)
            )
        }
        Timber.d("downloadPage end: chapterId=%s pageIndex=%d", chapterId, pageIndex)
    }

    private fun createForegroundInfo(
        chapterId: String,
        progressPercent: Int,
        indeterminate: Boolean
    ): ForegroundInfo {
        ensureNotificationChannel()
        val title = "Downloading chapter"
        val content = if (indeterminate) "Preparing download" else "$progressPercent%"
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent.coerceIn(0, 100), indeterminate)
            .build()

        return ForegroundInfo(notificationIdFor(chapterId), notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (existing != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Offline downloads",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun notificationIdFor(chapterId: String): Int {
        return NOTIFICATION_ID_BASE + (chapterId.hashCode().absoluteValue % NOTIFICATION_ID_RANGE)
    }

    private fun extensionFor(pageUrl: String, contentSubtype: String?): String {
        val subtype = contentSubtype?.lowercase()
        return when {
            subtype == "jpeg" || subtype == "jpg" -> "jpg"
            subtype == "png" -> "png"
            subtype == "webp" -> "webp"
            subtype == "gif" -> "gif"
            else -> pageUrl.substringBefore("?")
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
        private const val NOTIFICATION_CHANNEL_ID = "offline_downloads"
        private const val NOTIFICATION_ID_BASE = 41_000
        private const val NOTIFICATION_ID_RANGE = 1_000
    }
}
