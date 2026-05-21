package com.example.mybookslibrary.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade used by presentation code to enqueue or cancel offline chapter downloads.
 */
@Singleton
class OfflineDownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: OfflineDownloadRepository
) {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    suspend fun enqueueDownload(mangaId: String, chapterId: String) {
        val downloadOnlyOnWifi = repository.getDownloadOnlyOnWifi()
        val networkType = if (downloadOnlyOnWifi) NetworkType.UNMETERED else NetworkType.CONNECTED

        Timber.d(
            "enqueueDownload: mangaId=%s chapterId=%s networkType=%s",
            mangaId,
            chapterId,
            networkType
        )
        repository.enqueueChapter(mangaId, chapterId)

        val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()
            )
            .setInputData(
                workDataOf(
                    ChapterDownloadWorker.KEY_MANGA_ID to mangaId,
                    ChapterDownloadWorker.KEY_CHAPTER_ID to chapterId
                )
            )
            .addTag(CHAPTER_DOWNLOAD_TAG)
            .addTag(chapterTag(chapterId))
            .build()

        workManager.enqueueUniqueWork(uniqueWorkName(chapterId), ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun cancelDownload(chapterId: String) {
        Timber.d("cancelDownload: chapterId=%s", chapterId)
        workManager.cancelUniqueWork(uniqueWorkName(chapterId))
        repository.removeQueuedChapter(chapterId)
    }

    companion object {
        const val CHAPTER_DOWNLOAD_TAG = "offline_chapter_download"

        fun uniqueWorkName(chapterId: String): String = "offline_chapter_download_$chapterId"

        fun chapterTag(chapterId: String): String = "offline_chapter_download_chapter_$chapterId"
    }
}
