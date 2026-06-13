package com.example.mybookslibrary.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mybookslibrary.MainActivity
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.repository.MangaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker định kỳ kiểm tra feed các truyện trong thư viện và thông báo khi có chương mới nhất.
 *
 * Phát hiện "chương mới" bằng cách so id chương mới nhất (phần tử cuối của feed sắp xếp tăng dần)
 * với marker đã lưu trong DataStore. Lần đầu thấy 1 truyện → chỉ seed marker, KHÔNG thông báo
 * (tránh spam toàn bộ lịch sử khi vừa bật tính năng).
 */
@HiltWorker
class NewChapterCheckWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val libraryDao: LibraryDao,
        private val mangaRepository: MangaRepository,
        private val preferencesDataStore: UserPreferencesDataStore,
        private val json: Json,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            if (!preferencesDataStore.getNewChapterNotifications()) return Result.success()

            val items = libraryDao.getAll()
            if (items.isEmpty()) return Result.success()

            val seen = parseSeenMap(preferencesDataStore.getNewChapterSeenMapRaw()).toMutableMap()
            val mangaWithNewChapter = mutableListOf<String>()

            for (item in items) {
                // Lỗi mạng/feed cho 1 truyện → bỏ qua truyện đó (best-effort), không fail cả job.
                val latestChapterId = mangaRepository.getChapterFeed(item.manga_id)
                    .getOrNull()
                    ?.lastOrNull()
                    ?.id ?: continue

                val previousLatest = seen[item.manga_id]
                when {
                    previousLatest == null -> seen[item.manga_id] = latestChapterId // seed, không báo
                    previousLatest != latestChapterId -> {
                        mangaWithNewChapter += item.title
                        seen[item.manga_id] = latestChapterId
                    }
                }
            }

            // Dọn marker của truyện đã rời thư viện để map không phình vô hạn.
            seen.keys.retainAll(items.mapTo(mutableSetOf()) { it.manga_id })
            preferencesDataStore.setNewChapterSeenMapRaw(json.encodeToString(seen.toMap()))

            Timber.d(
                "NewChapterCheckWorker: checked=%d, có chương mới=%d",
                items.size,
                mangaWithNewChapter.size,
            )
            if (mangaWithNewChapter.isNotEmpty()) {
                showNotification(mangaWithNewChapter)
            }
            return Result.success()
        }

        private fun parseSeenMap(raw: String): Map<String, String> {
            if (raw.isBlank()) return emptyMap()
            return try {
                json.decodeFromString<Map<String, String>>(raw)
            } catch (t: Throwable) {
                Timber.w(t, "parseSeenMap lỗi, reset marker")
                emptyMap()
            }
        }

        private fun showNotification(mangaTitles: List<String>) {
            createChannel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val body = mangaTitles.joinToString(", ")
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_brand_logo)
                .setContentTitle(applicationContext.getString(R.string.notification_new_chapter_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent())
                .build()

            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
        }

        // Tap notification → mở app (màn mặc định). Deep-link tới MangaDetail cụ thể: backlog.
        private fun openAppIntent(): PendingIntent {
            val intent = Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun createChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_new_chapter_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = applicationContext.getString(R.string.notification_new_chapter_channel_desc)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        companion object {
            private const val CHANNEL_ID = "new_chapter"
            private const val NOTIFICATION_ID = 2002
            const val WORK_NAME = "new_chapter_check"

            /** Lên lịch kiểm tra chương mới mỗi 24h (cần mạng). Worker tự bỏ qua nếu toggle tắt. */
            fun schedule(context: Context) {
                val request = PeriodicWorkRequestBuilder<NewChapterCheckWorker>(
                    repeatInterval = 1,
                    repeatIntervalTimeUnit = TimeUnit.DAYS,
                ).setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            }
        }
    }
