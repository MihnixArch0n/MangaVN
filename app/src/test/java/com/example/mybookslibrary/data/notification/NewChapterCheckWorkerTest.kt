package com.example.mybookslibrary.data.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.ChapterModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Test [NewChapterCheckWorker.doWork] qua Robolectric — tập trung logic phát hiện chương mới
 * (marker map), không assert việc post notification (wrapper mỏng, phụ thuộc permission runtime).
 */
@RunWith(RobolectricTestRunner::class)
class NewChapterCheckWorkerTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val libraryDao = mockk<LibraryDao>(relaxed = true)
    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val json = Json

    @Before
    fun setUp() {
        // Robolectric không tự grant runtime permission → cần grant để showNotification chạy thật.
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun doWork_toggleOff_returnsSuccessAndDoesNotQueryFeed() =
        runBlocking {
            coEvery { preferencesDataStore.getNewChapterNotifications() } returns false

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            coVerify(exactly = 0) { mangaRepository.getChapterFeed(any()) }
        }

    @Test
    fun doWork_firstSeen_seedsMarkerWithoutNewChapterList() =
        runBlocking {
            stubEnabled(seenRaw = "")
            coEvery { libraryDao.getAll() } returns listOf(libraryItem("m1"))
            coEvery { mangaRepository.getChapterFeed("m1") } returns
                Result.success(listOf(chapter("c1"), chapter("c2"), chapter("c3")))
            val savedRaw = captureSavedMap()

            buildWorker().doWork()

            // Lần đầu thấy m1 → seed marker = chương cuối "c3", KHÔNG báo notification.
            assertEquals("c3", parseMap(savedRaw.captured)["m1"])
            assertEquals(0, activeNotificationCount())
        }

    @Test
    fun doWork_newChapter_updatesMarkerToLatest() =
        runBlocking {
            stubEnabled(seenRaw = json.encodeToString(mapOf("m1" to "c2")))
            coEvery { libraryDao.getAll() } returns listOf(libraryItem("m1"))
            coEvery { mangaRepository.getChapterFeed("m1") } returns
                Result.success(listOf(chapter("c1"), chapter("c2"), chapter("c3")))
            val savedRaw = captureSavedMap()

            buildWorker().doWork()

            // Chương cuối "c3" khác marker cũ "c2" → cập nhật marker + BÁO notification.
            assertEquals("c3", parseMap(savedRaw.captured)["m1"])
            assertEquals(1, activeNotificationCount())
        }

    @Test
    fun doWork_noChange_keepsMarker() =
        runBlocking {
            stubEnabled(seenRaw = json.encodeToString(mapOf("m1" to "c3")))
            coEvery { libraryDao.getAll() } returns listOf(libraryItem("m1"))
            coEvery { mangaRepository.getChapterFeed("m1") } returns
                Result.success(listOf(chapter("c1"), chapter("c2"), chapter("c3")))
            val savedRaw = captureSavedMap()

            buildWorker().doWork()

            // Không đổi → giữ marker "c3", KHÔNG báo.
            assertEquals("c3", parseMap(savedRaw.captured)["m1"])
            assertEquals(0, activeNotificationCount())
        }

    @Test
    fun doWork_feedFailure_skipsMangaAndDoesNotAddMarker() =
        runBlocking {
            stubEnabled(seenRaw = "")
            coEvery { libraryDao.getAll() } returns listOf(libraryItem("m1"))
            coEvery { mangaRepository.getChapterFeed("m1") } returns
                Result.failure(RuntimeException("network"))
            val savedRaw = captureSavedMap()

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            // Feed lỗi → không có marker cho m1 (best-effort, bỏ qua truyện đó).
            assertNull(parseMap(savedRaw.captured)["m1"])
        }

    private fun stubEnabled(seenRaw: String) {
        coEvery { preferencesDataStore.getNewChapterNotifications() } returns true
        coEvery { preferencesDataStore.getNewChapterSeenMapRaw() } returns seenRaw
    }

    private fun captureSavedMap(): io.mockk.CapturingSlot<String> {
        val slot = slot<String>()
        coEvery { preferencesDataStore.setNewChapterSeenMapRaw(capture(slot)) } returns Unit
        return slot
    }

    private fun parseMap(raw: String): Map<String, String> =
        if (raw.isBlank()) emptyMap() else json.decodeFromString(raw)

    private fun activeNotificationCount(): Int =
        shadowOf(context.getSystemService(NotificationManager::class.java)).size()

    private fun libraryItem(mangaId: String) =
        LibraryItemEntity(manga_id = mangaId, title = "Manga $mangaId", cover_url = "")

    private fun chapter(id: String) =
        ChapterModel(
            id = id,
            mangaId = "m1",
            volume = null,
            chapterNumber = id.removePrefix("c"),
            title = null,
            pages = 10,
            isUnavailable = false,
        )

    private fun buildWorker(): NewChapterCheckWorker =
        TestListenableWorkerBuilder<NewChapterCheckWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        NewChapterCheckWorker(
                            appContext,
                            workerParameters,
                            libraryDao,
                            mangaRepository,
                            preferencesDataStore,
                            json,
                        )
                },
            ).build()
}
