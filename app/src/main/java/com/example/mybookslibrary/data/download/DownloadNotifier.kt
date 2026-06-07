package com.example.mybookslibrary.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
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
        fun createForegroundInfo(
            chapterId: String,
            progressPercent: Int,
            indeterminate: Boolean,
        ): ForegroundInfo {
            ensureNotificationChannel()
            val content = if (indeterminate) "Preparing download" else "$progressPercent%"
            val notification =
                NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle("Downloading chapter")
                    .setContentText(content)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setProgress(100, progressPercent.coerceIn(0, 100), indeterminate)
                    .build()

            return ForegroundInfo(
                notificationIdFor(chapterId),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        }

        @ExcludeFromGeneratedCoverage // NotificationManager Android glue
        fun ensureNotificationChannel() {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (existing != null) return

            manager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Offline downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }

        @ExcludeFromGeneratedCoverage // NotificationManagerCompat + permission/SecurityException Android glue
        fun showFinishedNotification(
            chapterId: String,
            success: Boolean,
            message: String,
        ) {
            ensureNotificationChannel()
            val notification =
                NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(if (success) "Download complete" else "Download failed")
                    .setContentText(message)
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

        private fun notificationIdFor(chapterId: String): Int =
            NOTIFICATION_ID_BASE + (chapterId.hashCode().absoluteValue % NOTIFICATION_ID_RANGE)

        private fun finishedNotificationIdFor(chapterId: String): Int =
            FINISHED_NOTIFICATION_ID_BASE + (chapterId.hashCode().absoluteValue % NOTIFICATION_ID_RANGE)

        private companion object {
            const val NOTIFICATION_CHANNEL_ID = "offline_downloads"
            const val NOTIFICATION_ID_BASE = 41_000
            const val FINISHED_NOTIFICATION_ID_BASE = 42_000
            const val NOTIFICATION_ID_RANGE = 1_000
        }
    }
