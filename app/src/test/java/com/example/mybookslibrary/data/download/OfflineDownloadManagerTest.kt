package com.example.mybookslibrary.data.download

import android.content.Context
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OfflineDownloadManagerTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        // WorkManager thật dạng test (đồng bộ) -> OfflineDownloadManager.workManager hoạt động
        // mà không phải mock static getInstance (mock static gây autohint gọi impl thật -> crash).
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun buildDownloadRequest_usesUnmeteredConstraintForWifiOnly() {
        val request =
            manager().buildDownloadRequest(
                mangaId = MANGA_ID,
                chapterId = CHAPTER_ID,
                networkType = NetworkType.UNMETERED,
                mangaTitle = "One Piece",
                chapterLabel = "Vol. 1 Ch. 2",
            )

        assertEquals(NetworkType.UNMETERED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(MANGA_ID, request.workSpec.input.getString(ChapterDownloadWorker.KEY_MANGA_ID))
        assertEquals(CHAPTER_ID, request.workSpec.input.getString(ChapterDownloadWorker.KEY_CHAPTER_ID))
        assertEquals("One Piece", request.workSpec.input.getString(ChapterDownloadWorker.KEY_MANGA_TITLE))
        assertEquals("Vol. 1 Ch. 2", request.workSpec.input.getString(ChapterDownloadWorker.KEY_CHAPTER_LABEL))
        assertTrue(OfflineDownloadManager.CHAPTER_DOWNLOAD_TAG in request.tags)
        assertTrue(OfflineDownloadManager.chapterTag(CHAPTER_ID) in request.tags)
    }

    @Test
    fun buildDownloadRequest_usesConnectedConstraintWhenWifiOnlyDisabled() {
        val request =
            manager().buildDownloadRequest(
                mangaId = MANGA_ID,
                chapterId = CHAPTER_ID,
                networkType = NetworkType.CONNECTED,
            )

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun enqueueDownload_wifiOnly_enqueueChapterVaUniqueWork() =
        runBlocking {
            val repository = mockk<OfflineDownloadRepository>(relaxed = true)
            coEvery { repository.getDownloadOnlyOnWifi() } returns true

            manager(repository).enqueueDownload(MANGA_ID, CHAPTER_ID)

            coVerify { repository.enqueueChapter(MANGA_ID, CHAPTER_ID) }
            val workInfos =
                WorkManager
                    .getInstance(context)
                    .getWorkInfosForUniqueWork(OfflineDownloadManager.uniqueWorkName(CHAPTER_ID))
                    .get()
            assertTrue(workInfos.isNotEmpty())
        }

    @Test
    fun enqueueDownload_khongWifiOnly_dungConnected() =
        runBlocking {
            val repository = mockk<OfflineDownloadRepository>(relaxed = true)
            coEvery { repository.getDownloadOnlyOnWifi() } returns false

            manager(repository).enqueueDownload(MANGA_ID, CHAPTER_ID)

            coVerify { repository.enqueueChapter(MANGA_ID, CHAPTER_ID) }
            val workInfos =
                WorkManager
                    .getInstance(context)
                    .getWorkInfosForUniqueWork(OfflineDownloadManager.uniqueWorkName(CHAPTER_ID))
                    .get()
            assertTrue(workInfos.isNotEmpty())
        }

    @Test
    fun cancelDownload_cancelWorkVaRemoveQueued() =
        runBlocking {
            val repository = mockk<OfflineDownloadRepository>(relaxed = true)

            manager(repository).cancelDownload(MANGA_ID, CHAPTER_ID)

            coVerify { repository.removeQueuedChapter(CHAPTER_ID) }
        }

    @Test
    fun deleteDownload_cancelWorkXoaStorageVaMarkNotDownloaded() =
        runBlocking {
            val repository = mockk<OfflineDownloadRepository>(relaxed = true)
            val storage = mockk<OfflineDownloadStorage>(relaxed = true)

            manager(repository, storage).deleteDownload(MANGA_ID, CHAPTER_ID)

            coVerify { storage.deleteChapter(MANGA_ID, CHAPTER_ID) }
            coVerify { repository.markChapterNotDownloaded(CHAPTER_ID) }
        }

    private fun manager(
        repository: OfflineDownloadRepository = mockk(relaxed = true),
        storage: OfflineDownloadStorage = mockk(relaxed = true),
    ): OfflineDownloadManager =
        OfflineDownloadManager(
            context = context,
            repository = repository,
            storage = storage,
        )

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
