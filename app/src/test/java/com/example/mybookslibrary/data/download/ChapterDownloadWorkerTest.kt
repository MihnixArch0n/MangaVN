package com.example.mybookslibrary.data.download

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.repository.ChapterDelivery
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Test [ChapterDownloadWorker.doWork] qua Robolectric:
 * - tải hết trang → ghi marker complete + Result.success;
 * - chapter không có trang → Result.failure + queue ERROR (không crash);
 * - bị huỷ → re-throw CancellationException, KHÔNG ghi trạng thái ERROR.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChapterDownloadWorkerTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val offlineDownloadRepository = mockk<OfflineDownloadRepository>(relaxed = true)
    private lateinit var storage: OfflineDownloadStorage
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        storage = OfflineDownloadStorage(context, UnconfinedTestDispatcher())
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() =
        runBlocking {
            mockWebServer.shutdown()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
        }

    @Test
    fun doWork_downloadsAllPages_marksCompleteAndSucceeds() =
        runBlocking {
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            mockWebServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        MockResponse().setResponseCode(200).setBody("page-bytes")
                }
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns Result.success(delivery())

            val result = buildWorker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertTrue(CHAPTER_ID in storage.scanDownloadedChapters())
            coVerify {
                offlineDownloadRepository.markChapterDownloaded(
                    mangaId = MANGA_ID,
                    chapterId = CHAPTER_ID,
                    totalPages = 2,
                )
            }
        }

    @Test
    fun doWork_noPages_failsAndMarksError() =
        runBlocking {
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns
                Result.success(delivery(filenames = emptyList()))

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            assertFalse(CHAPTER_ID in storage.scanDownloadedChapters())
            coVerify {
                offlineDownloadRepository.updateQueueStatus(
                    chapterId = CHAPTER_ID,
                    status = DownloadStatus.ERROR,
                    progressPercent = 0,
                    errorMessage = any(),
                )
            }
        }

    @Test
    fun doWork_whenCancelled_rethrowsAndDoesNotMarkError() =
        runBlocking {
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } throws CancellationException()

            var thrown: Throwable? = null
            try {
                buildWorker().doWork()
            } catch (cancellation: CancellationException) {
                thrown = cancellation
            }

            assertTrue(thrown is CancellationException)
            coVerify(exactly = 0) {
                offlineDownloadRepository.updateQueueStatus(
                    chapterId = CHAPTER_ID,
                    status = DownloadStatus.ERROR,
                    progressPercent = any(),
                    errorMessage = any(),
                )
            }
        }

    private fun buildWorker(): ChapterDownloadWorker =
        TestListenableWorkerBuilder<ChapterDownloadWorker>(
            context = context,
            inputData =
                workDataOf(
                    ChapterDownloadWorker.KEY_MANGA_ID to MANGA_ID,
                    ChapterDownloadWorker.KEY_CHAPTER_ID to CHAPTER_ID,
                ),
        ).setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    ChapterDownloadWorker(
                        appContext,
                        workerParameters,
                        mangaRepository,
                        offlineDownloadRepository,
                        storage,
                        OkHttpClient(),
                    )
            },
        ).build()

    private fun delivery(filenames: List<String> = listOf("page-1.png", "page-2.png")): ChapterDelivery =
        ChapterDelivery(
            baseUrl = mockWebServer.url("/").toString().trimEnd('/'),
            quality = "data",
            hash = "hash",
            filenames = filenames,
        )

    private companion object {
        const val MANGA_ID = "worker-test-manga"
        const val CHAPTER_ID = "worker-test-chapter"
    }
}
