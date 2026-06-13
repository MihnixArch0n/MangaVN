package com.example.mybookslibrary.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import com.example.mybookslibrary.MainActivity
import com.example.mybookslibrary.R
import com.example.mybookslibrary.util.ExcludeFromGeneratedCoverage
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * Android notification glue for offline chapter downloads.
 */
@Singleton
class DownloadNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        @ExcludeFromGeneratedCoverage // Notification/ForegroundInfo + Android foreground-service glue
        internal fun createForegroundInfo(
            chapterId: String,
            mangaTitle: String? = null,
            chapterLabel: String? = null,
            progressPercent: Int,
            indeterminate: Boolean,
        ): ForegroundInfo {
            ensureNotificationChannel()
            val progressText =
                if (indeterminate) {
                    context.getString(R.string.notification_download_preparing)
                } else {
                    "$progressPercent%"
                }
            val title = mangaTitle.nonBlankOrNull() ?: context.getString(R.string.notification_download_in_progress)
            val content = joinNotificationDetails(chapterLabel, progressText)
            val notification =
                NotificationCompat
                    .Builder(context, DOWNLOAD_PROGRESS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .setProgress(
                        PROGRESS_MAX,
                        progressPercent.coerceIn(PROGRESS_MIN, PROGRESS_MAX),
                        indeterminate,
                    )
                    .build()

            return ForegroundInfo(
                notificationIdFor(chapterId),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        }

        @ExcludeFromGeneratedCoverage // NotificationManager Android glue
        private fun ensureNotificationChannel() {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        DOWNLOAD_PROGRESS_CHANNEL_ID,
                        context.getString(R.string.notification_download_progress_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                    NotificationChannel(
                        DOWNLOAD_RESULT_CHANNEL_ID,
                        context.getString(R.string.notification_download_result_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                ),
            )
        }

        @ExcludeFromGeneratedCoverage // NotificationManager Android glue
        private fun ensureDownloadResultChannel() {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    DOWNLOAD_RESULT_CHANNEL_ID,
                    context.getString(R.string.notification_download_result_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        @ExcludeFromGeneratedCoverage // NotificationManagerCompat + permission/SecurityException Android glue
        internal fun showFinishedNotification(
            mangaId: String,
            chapterId: String,
            mangaTitle: String? = null,
            chapterLabel: String? = null,
            success: Boolean,
        ) {
            ensureDownloadResultChannel()
            // Text qua resources (i18n) + generic — không expose exception message nội bộ
            val title =
                context.getString(
                    if (success) R.string.notification_download_complete else R.string.notification_download_failed,
                )
            val notification =
                NotificationCompat
                    .Builder(context, DOWNLOAD_RESULT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(title)
                    .setContentText(joinNotificationDetails(mangaTitle, chapterLabel))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(joinNotificationDetails(mangaTitle, chapterLabel)),
                    )
                    .setContentIntent(
                        if (success) context.readPendingIntent(mangaId, chapterId, chapterLabel) else null,
                    )
                    .addAction(
                        if (success) {
                            notificationAction(
                                title = context.getString(R.string.notification_action_read),
                                pendingIntent = context.readPendingIntent(mangaId, chapterId, chapterLabel),
                            )
                        } else {
                            notificationAction(
                                title = context.getString(R.string.notification_action_retry),
                                pendingIntent =
                                    context.retryPendingIntent(mangaId, chapterId, mangaTitle, chapterLabel),
                            )
                        },
                    )
                    .addAction(
                        notificationAction(
                            title = context.getString(R.string.notification_action_dismiss),
                            pendingIntent = context.dismissPendingIntent(chapterId),
                        ),
                    )
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .build()

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(finishedNotificationIdFor(chapterId), notification)
                } else {
                    Timber.w("Finished notification skipped: notifications disabled chapterId=%s", chapterId)
                }
            } catch (securityException: SecurityException) {
                Timber.w(securityException, "Finished notification skipped: missing notification permission")
            }
        }

        internal fun dismissFinishedNotification(chapterId: String) {
            NotificationManagerCompat.from(context).cancel(finishedNotificationIdFor(chapterId))
        }

        private fun notificationIdFor(chapterId: String): Int =
            NOTIFICATION_ID_BASE + (chapterId.hashCode().absoluteValue % NOTIFICATION_ID_RANGE)

        private fun finishedNotificationIdFor(chapterId: String): Int =
            FINISHED_NOTIFICATION_ID_BASE + (chapterId.hashCode().absoluteValue % NOTIFICATION_ID_RANGE)

        private fun joinNotificationDetails(first: String?, second: String?): String =
            listOfNotNull(first.nonBlankOrNull(), second.nonBlankOrNull()).joinToString(DETAIL_SEPARATOR)

        private fun String?.nonBlankOrNull(): String? = this?.takeIf { it.isNotBlank() }

        private companion object {
            // Keep the existing ID so installed users retain their progress-channel preferences.
            const val DOWNLOAD_PROGRESS_CHANNEL_ID = "offline_downloads"

            // A separate ID is required because Android doesn't allow raising an existing channel's importance.
            const val DOWNLOAD_RESULT_CHANNEL_ID = "offline_download_results"
            const val NOTIFICATION_ID_BASE = 41_000
            const val FINISHED_NOTIFICATION_ID_BASE = 42_000
            const val NOTIFICATION_ID_RANGE = 1_000
            const val PROGRESS_MIN = 0
            const val PROGRESS_MAX = 100
            const val DETAIL_SEPARATOR = " · "
        }
    }

private fun notificationAction(
    title: String,
    pendingIntent: PendingIntent,
): NotificationCompat.Action =
    NotificationCompat.Action.Builder(R.drawable.ic_stat_name, title, pendingIntent).build()

private fun Context.readPendingIntent(
    mangaId: String,
    chapterId: String,
    chapterLabel: String?,
): PendingIntent =
    PendingIntent.getActivity(
        this,
        actionRequestCode(chapterId, READ_ACTION_REQUEST_OFFSET),
        Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_READ_DOWNLOADED_CHAPTER
            putExtra(MainActivity.EXTRA_MANGA_ID, mangaId)
            putExtra(MainActivity.EXTRA_CHAPTER_ID, chapterId)
            putExtra(MainActivity.EXTRA_CHAPTER_TITLE, chapterLabel.orEmpty())
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private fun Context.retryPendingIntent(
    mangaId: String,
    chapterId: String,
    mangaTitle: String?,
    chapterLabel: String?,
): PendingIntent =
    broadcastPendingIntent(
        action = DownloadNotificationActionReceiver.ACTION_RETRY,
        chapterId = chapterId,
        requestOffset = RETRY_ACTION_REQUEST_OFFSET,
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        chapterLabel = chapterLabel,
    )

private fun Context.dismissPendingIntent(chapterId: String): PendingIntent =
    broadcastPendingIntent(
        action = DownloadNotificationActionReceiver.ACTION_DISMISS,
        chapterId = chapterId,
        requestOffset = DISMISS_ACTION_REQUEST_OFFSET,
    )

private fun Context.broadcastPendingIntent(
    action: String,
    chapterId: String,
    requestOffset: Int,
    mangaId: String? = null,
    mangaTitle: String? = null,
    chapterLabel: String? = null,
): PendingIntent =
    PendingIntent.getBroadcast(
        this,
        actionRequestCode(chapterId, requestOffset),
        Intent(this, DownloadNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(DownloadNotificationActionReceiver.EXTRA_CHAPTER_ID, chapterId)
            putExtra(DownloadNotificationActionReceiver.EXTRA_MANGA_ID, mangaId)
            putExtra(DownloadNotificationActionReceiver.EXTRA_MANGA_TITLE, mangaTitle)
            putExtra(DownloadNotificationActionReceiver.EXTRA_CHAPTER_LABEL, chapterLabel)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private fun actionRequestCode(chapterId: String, offset: Int): Int =
    offset + (chapterId.hashCode().absoluteValue % ACTION_REQUEST_ID_RANGE)

private const val ACTION_REQUEST_ID_RANGE = 1_000
private const val READ_ACTION_REQUEST_OFFSET = 51_000
private const val RETRY_ACTION_REQUEST_OFFSET = 52_000
private const val DISMISS_ACTION_REQUEST_OFFSET = 53_000
