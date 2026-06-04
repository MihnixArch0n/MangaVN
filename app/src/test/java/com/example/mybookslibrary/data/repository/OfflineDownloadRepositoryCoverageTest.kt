package com.example.mybookslibrary.data.repository

import androidx.room.Room
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Phủ thao tác queue + cài đặt của [OfflineDownloadRepository]: enqueue/update/remove/observe
 * (Room in-memory) và delegate cài đặt download-only-on-wifi sang DataStore (mock).
 */
@RunWith(RobolectricTestRunner::class)
class OfflineDownloadRepositoryCoverageTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: OfflineDownloadRepository
    private val cache = mockk<DownloadedChapterCache>(relaxed = true)
    private val prefs = mockk<UserPreferencesDataStore>(relaxed = true)

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository =
            OfflineDownloadRepository(
                downloadQueueDao = db.downloadQueueDao(),
                chapterDao = db.chapterDao(),
                database = db,
                preferencesDataStore = prefs,
                downloadedChapterCache = cache,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun enqueueChapter_addsPendingEntry() =
        runTest {
            repository.enqueueChapter("m1", "c1")

            val entry = repository.getQueueByChapter("c1")!!
            assertEquals(DownloadStatus.PENDING, entry.status)
            assertEquals(0, entry.progress_percent)
        }

    @Test
    fun updateQueueStatus_persistsStatusAndClampsProgress() =
        runTest {
            repository.enqueueChapter("m1", "c1")

            repository.updateQueueStatus("c1", DownloadStatus.DOWNLOADING, progressPercent = 150)

            val entry = repository.getQueueByChapter("c1")!!
            assertEquals(DownloadStatus.DOWNLOADING, entry.status)
            assertEquals(100, entry.progress_percent) // 150 phải bị kẹp về 100
        }

    @Test
    fun removeQueuedChapter_removesEntry() =
        runTest {
            repository.enqueueChapter("m1", "c1")

            repository.removeQueuedChapter("c1")

            assertNull(repository.getQueueByChapter("c1"))
        }

    @Test
    fun removeQueuedManga_removesAllEntriesOfManga() =
        runTest {
            repository.enqueueChapter("m1", "c1")
            repository.enqueueChapter("m1", "c2")

            repository.removeQueuedManga("m1")

            assertNull(repository.getQueueByChapter("c1"))
            assertNull(repository.getQueueByChapter("c2"))
        }

    @Test
    fun observeQueue_emitsEnqueuedChapters() =
        runTest {
            repository.enqueueChapter("m1", "c1")

            val queue = repository.observeQueue().first()

            assertEquals(listOf("c1"), queue.map { it.chapter_id })
        }

    @Test
    fun setDownloadOnlyOnWifi_delegatesToDataStore() =
        runTest {
            repository.setDownloadOnlyOnWifi(true)

            coVerify { prefs.setDownloadOnlyOnWifi(true) }
        }

    @Test
    fun getDownloadOnlyOnWifi_returnsDataStoreValue() =
        runTest {
            coEvery { prefs.getDownloadOnlyOnWifi() } returns true

            assertTrue(repository.getDownloadOnlyOnWifi())
        }

    @Test
    fun observeQueueByManga_emitsChaptersOfManga() =
        runTest {
            repository.enqueueChapter("m1", "c1")

            val queue = repository.observeQueueByManga("m1").first()

            assertEquals(listOf("c1"), queue.map { it.chapter_id })
        }

    @Test
    fun observeQueueByChapter_emitsEntry() =
        runTest {
            repository.enqueueChapter("m1", "c1")

            assertEquals("c1", repository.observeQueueByChapter("c1").first()?.chapter_id)
        }

    @Test
    fun observeDownloadOnlyOnWifi_delegatesToDataStore() =
        runTest {
            every { prefs.observeDownloadOnlyOnWifi() } returns flowOf(true)

            assertTrue(repository.observeDownloadOnlyOnWifi().first())
        }

    @Test
    fun markChapterDownloaded_withDefaultTotalPages_createsProgress() =
        runTest {
            db.libraryDao().upsert(LibraryItemEntity(manga_id = "m1", title = "T", cover_url = ""))

            repository.markChapterDownloaded("m1", "c1")

            assertNotNull(db.chapterDao().getChapterProgressByChapter("c1"))
        }
}
