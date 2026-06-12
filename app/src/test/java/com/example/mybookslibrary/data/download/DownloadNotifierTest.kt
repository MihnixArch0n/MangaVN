package com.example.mybookslibrary.data.download

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadNotifierTest {
    private lateinit var context: Context
    private lateinit var notifier: DownloadNotifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notifier = DownloadNotifier(context)
    }

    @Test
    fun createForegroundInfo_usesDataSyncForegroundServiceType() {
        val info = notifier.createForegroundInfo("chapter-1", progressPercent = 50, indeterminate = false)

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, info.foregroundServiceType)
    }

    @Test
    fun createForegroundInfo_producesStableIdInProgressRange() {
        val first = notifier.createForegroundInfo("chapter-1", progressPercent = 0, indeterminate = true)
        val second = notifier.createForegroundInfo("chapter-1", progressPercent = 80, indeterminate = false)

        assertEquals(first.notificationId, second.notificationId)
        assertTrue(first.notificationId in 41_000 until 42_000)
    }

    @Test
    fun createForegroundInfo_clampsProgressIntoValidRange() {
        val info = notifier.createForegroundInfo("chapter-1", progressPercent = 250, indeterminate = false)

        assertEquals(100, info.notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS))
        assertEquals(100, info.notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX))
    }

    @Test
    fun createForegroundInfo_marksIndeterminatePreparingState() {
        val info = notifier.createForegroundInfo("chapter-1", progressPercent = 0, indeterminate = true)

        assertTrue(info.notification.extras.getBoolean(NotificationCompat.EXTRA_PROGRESS_INDETERMINATE))
    }

    @Test
    fun createForegroundInfo_createsNotificationChannel() {
        notifier.createForegroundInfo("chapter-1", progressPercent = 0, indeterminate = true)

        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager.getNotificationChannel("offline_downloads"))
    }

    @Test
    fun showFinishedNotification_postsWithIdOutsideProgressRange() {
        notifier.showFinishedNotification("chapter-1", success = true)

        val manager = context.getSystemService(NotificationManager::class.java)
        val posted = manager.activeNotifications.single()
        assertTrue(posted.id in 42_000 until 43_000)
    }

    @Test
    fun showFinishedNotification_doesNotCollideWithProgressNotificationId() {
        val progressId =
            notifier.createForegroundInfo("chapter-1", progressPercent = 0, indeterminate = true).notificationId

        notifier.showFinishedNotification("chapter-1", success = true)

        val manager = context.getSystemService(NotificationManager::class.java)
        assertTrue(manager.activeNotifications.none { it.id == progressId })
    }
}
