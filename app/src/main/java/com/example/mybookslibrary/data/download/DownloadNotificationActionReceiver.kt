package com.example.mybookslibrary.data.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mybookslibrary.di.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DownloadNotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var downloadManager: OfflineDownloadManager

    @Inject
    lateinit var downloadNotifier: DownloadNotifier

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID).orEmpty()
        if (chapterId.isBlank()) return

        downloadNotifier.dismissFinishedNotification(chapterId)
        if (intent.action != ACTION_RETRY) return

        val mangaId = intent.getStringExtra(EXTRA_MANGA_ID).orEmpty()
        if (mangaId.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            try {
                downloadManager.enqueueDownload(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    mangaTitle = intent.getStringExtra(EXTRA_MANGA_TITLE),
                    chapterLabel = intent.getStringExtra(EXTRA_CHAPTER_LABEL),
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (e: Exception) {
                Timber.e(e, "Retry chapter download failed to enqueue: chapterId=%s", chapterId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_RETRY = "com.example.mybookslibrary.action.RETRY_CHAPTER_DOWNLOAD"
        const val ACTION_DISMISS = "com.example.mybookslibrary.action.DISMISS_DOWNLOAD_NOTIFICATION"
        const val EXTRA_MANGA_ID = "manga_id"
        const val EXTRA_CHAPTER_ID = "chapter_id"
        const val EXTRA_MANGA_TITLE = "manga_title"
        const val EXTRA_CHAPTER_LABEL = "chapter_label"
    }
}
