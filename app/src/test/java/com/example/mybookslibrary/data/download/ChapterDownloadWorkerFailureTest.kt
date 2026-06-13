@file:Suppress(
    "ForbiddenComment",
    "ktlint:standard:function-signature",
    "ktlint:standard:indent",
    "ktlint:standard:max-line-length",
)

package com.example.mybookslibrary.data.download

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.repository.ChapterDelivery
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Phủ các nhánh LỖI của [ChapterDownloadWorker.doWork] mà happy-path test không chạm:
 * input rỗng, HTTP lỗi → retry → exhaust + failover, IOException mạng, chọn extension
 * theo Content-Type, và self-heal khi markChapterDownloaded ném sau khi đã ghi marker.
 */
// TODO: Refactor and reformat the legacy worker failure scenarios, then remove these ktlint suppressions.
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChapterDownloadWorkerFailureTest {
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
    fun doWork_inputRong_traVeFailureKhongGoiRepository() =
        runBlocking {
            val worker =
                TestListenableWorkerBuilder<ChapterDownloadWorker>(
                    context = context,
                    inputData =
                        workDataOf(
                            ChapterDownloadWorker.KEY_MANGA_ID to "",
                            ChapterDownloadWorker.KEY_CHAPTER_ID to "",
                        ),
                ).setWorkerFactory(factory()).build()

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            coVerify(exactly = 0) { mangaRepository.getChapterDelivery(any()) }
        }

    @Test
    fun doWork_chiChapterIdRong_traVeFailure() =
        runBlocking {
            // mangaId hợp lệ nhưng chapterId rỗng -> nhánh phải của `||` ở guard input.
            val worker =
                TestListenableWorkerBuilder<ChapterDownloadWorker>(
                    context = context,
                    inputData =
                        workDataOf(
                            ChapterDownloadWorker.KEY_MANGA_ID to MANGA_ID,
                            ChapterDownloadWorker.KEY_CHAPTER_ID to "",
                        ),
                ).setWorkerFactory(factory()).build()

            assertTrue(worker.doWork() is ListenableWorker.Result.Failure)
        }

    @Test
    fun doWork_exceptionMessageNull_vanFailureKhongCrash() =
        runBlocking {
            // Exception không có message -> nhánh `t.message ?: "..."` (null) ở showFinished + Result.failure.
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns
                Result.failure(RuntimeException())

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
        }

    @Test
    fun doWork_httpLoi_retryRoiThatBaiVaGhiError() =
        runBlocking {
            mockWebServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(500).setBody("err")
                }
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns
                Result.success(delivery(filenames = listOf("p1.png")))

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            coVerify {
                offlineDownloadRepository.updateQueueStatus(
                    chapterId = CHAPTER_ID,
                    status = DownloadStatus.ERROR,
                    progressPercent = 0,
                    errorMessage = any(),
                )
            }
            // failover refresh được gọi lại (>1 lần getChapterDelivery)
            coVerify(atLeast = 2) { mangaRepository.getChapterDelivery(CHAPTER_ID) }
        }

    @Test
    fun doWork_ioExceptionMang_traVeRetryVaKhongGhiError() =
        runBlocking {
            // Server đã tắt -> kết nối bị từ chối -> IOException ở downloadPage.
            val deadServer = MockWebServer()
            deadServer.start()
            val deadBaseUrl = deadServer.url("/").toString().trimEnd('/')
            deadServer.shutdown()
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns
                Result.success(
                    ChapterDelivery(
                        baseUrl = deadBaseUrl,
                        quality = "data",
                        hash = "hash",
                        filenames = listOf("p1.png"),
                    ),
                )
            coEvery { offlineDownloadRepository.getQueueByChapter(CHAPTER_ID) } returns
                DownloadQueueEntity(
                    chapter_id = CHAPTER_ID,
                    manga_id = MANGA_ID,
                    status = DownloadStatus.DOWNLOADING,
                    progress_percent = 40,
                )

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Retry)
            coVerify {
                offlineDownloadRepository.updateQueueStatus(
                    chapterId = CHAPTER_ID,
                    status = DownloadStatus.PENDING,
                    progressPercent = 40,
                )
            }
            coVerify(exactly = 0) {
                offlineDownloadRepository.updateQueueStatus(
                    chapterId = CHAPTER_ID,
                    status = DownloadStatus.ERROR,
                    progressPercent = any(),
                    errorMessage = any(),
                )
            }
        }

    @Test
    fun doWork_chonExtensionTheoContentType_vaPhatHienCacheHit() =
        runBlocking {
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            mockWebServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        val contentType =
                            when {
                                request.path?.endsWith(".jpg") == true -> "image/jpeg"
                                request.path?.endsWith(".jpeg") == true -> "image/jpg"
                                request.path?.endsWith(".png") == true -> "image/png"
                                request.path?.endsWith(".webp") == true -> "image/webp"
                                request.path?.endsWith(".gif") == true -> "image/gif"
                                else -> "application/octet-stream"
                            }
                        return MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", contentType)
                            // X-Cache: HIT -> nhánh cached = true ở downloadPage
                            .setHeader("X-Cache", "HIT")
                            .setBody("bytes")
                    }
                }
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns
                Result.success(
                    delivery(
                        filenames = listOf("p1.jpg", "p2.jpeg", "p3.png", "p4.webp", "p5.gif", "p6.bin"),
                    ),
                )

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            assertTrue(CHAPTER_ID in storage.scanDownloadedChapters())
        }

    @Test
    fun doWork_thieuKeyInput_traVeFailure() =
        runBlocking {
            // Không truyền key -> getString trả null -> nhánh null của orEmpty()
            val worker =
                TestListenableWorkerBuilder<ChapterDownloadWorker>(
                    context = context,
                    inputData = workDataOf(),
                ).setWorkerFactory(factory()).build()

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            coVerify(exactly = 0) { mangaRepository.getChapterDelivery(any()) }
        }

    @Test
    fun doWork_markDownloadedNem_vanSuccessNhoMarker() =
        runBlocking {
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            mockWebServer.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(200).setBody("bytes")
                }
            coEvery { mangaRepository.getChapterDelivery(CHAPTER_ID) } returns
                Result.success(delivery(filenames = listOf("p1.png")))
            // DB lỗi sau khi marker đã ghi -> worker nuốt lỗi, vẫn success (self-heal).
            coEvery {
                offlineDownloadRepository.markChapterDownloaded(any(), any(), any())
            } throws RuntimeException("db down")

            val result = buildWorker().doWork()

            assertTrue(result is ListenableWorker.Result.Success)
        }

    private fun buildWorker(): ChapterDownloadWorker =
        TestListenableWorkerBuilder<ChapterDownloadWorker>(
            context = context,
            inputData =
                workDataOf(
                    ChapterDownloadWorker.KEY_MANGA_ID to MANGA_ID,
                    ChapterDownloadWorker.KEY_CHAPTER_ID to CHAPTER_ID,
                ),
        ).setWorkerFactory(factory()).build()

    private fun factory(): WorkerFactory =
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
                    DownloadNotifier(appContext),
                    PageDownloader(
                        offlineDownloadStorage = storage,
                        imageOkHttpClient = OkHttpClient(),
                        ioDispatcher = UnconfinedTestDispatcher(),
                    ),
                )
        }

    private fun delivery(filenames: List<String>): ChapterDelivery =
        ChapterDelivery(
            baseUrl = mockWebServer.url("/").toString().trimEnd('/'),
            quality = "data",
            hash = "hash",
            filenames = filenames,
        )

    private companion object {
        const val MANGA_ID = "worker-fail-manga"
        const val CHAPTER_ID = "worker-fail-chapter"
    }
}
