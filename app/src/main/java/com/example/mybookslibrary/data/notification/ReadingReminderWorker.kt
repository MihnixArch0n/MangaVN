package com.example.mybookslibrary.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.dao.LibraryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReadingReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val libraryDao: LibraryDao,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val items = libraryDao.getAll()
            if (items.isEmpty()) return Result.success()

            val lastRead = items
                .filter { it.last_read_chapter_id != null }
                .maxByOrNull { it.updated_at }
                ?.updated_at ?: return Result.success()

            val daysSinceLastRead = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - lastRead,
            )

            if (daysSinceLastRead >= DAYS_THRESHOLD) {
                showNotification()
            }
            return Result.success()
        }

        private fun showNotification() {
            createChannel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_brand_logo)
                .setContentTitle(applicationContext.getString(R.string.notification_reminder_title))
                .setContentText(applicationContext.getString(R.string.notification_reminder_body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
        }

        private fun createChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = applicationContext.getString(R.string.notification_channel_desc)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        companion object {
            private const val CHANNEL_ID = "reading_reminder"
            private const val NOTIFICATION_ID = 2001
            private const val DAYS_THRESHOLD = 3L
            const val WORK_NAME = "reading_reminder"

            /** Lên lịch check hàng ngày — nếu đã 3 ngày chưa đọc → push notification. */
            fun schedule(context: Context) {
                val request = PeriodicWorkRequestBuilder<ReadingReminderWorker>(
                    repeatInterval = 1,
                    repeatIntervalTimeUnit = TimeUnit.DAYS,
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            }
        }
    }
