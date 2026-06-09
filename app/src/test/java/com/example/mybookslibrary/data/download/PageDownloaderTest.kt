package com.example.mybookslibrary.data.download

import android.content.Context
import com.example.mybookslibrary.data.remote.AtHomeReportPolicy
import com.example.mybookslibrary.data.repository.ChapterDelivery
import com.example.mybookslibrary.data.repository.MangaRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PageDownloaderTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val mangaRepository = mockk<MangaRepository>(relaxed = true)

    private lateinit var storage: OfflineDownloadStorage
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        storage = OfflineDownloadStorage(context, UnconfinedTestDispatcher())
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() =
        runTest {
            server.shutdown()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            storage.deleteChapter(MANGA_ID, FAILOVER_CHAPTER_ID)
            storage.deleteChapter(MANGA_ID, EXTENSION_CHAPTER_ID)
        }

    @Test
    fun downloadPageWithFailover_success_savesPageAndReportsAtHomeRequest() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "image/png")
                    .setBody("page-bytes"),
            )
            val coordinator = coordinator(delivery(server, filenames = listOf("page-1.png")))

            downloader().downloadPageWithFailover(
                mangaId = MANGA_ID,
                chapterId = CHAPTER_ID,
                pageIndex = 0,
                failoverCoordinator = coordinator,
            )

            val pages = storage.getChapterPages(MANGA_ID, CHAPTER_ID)
            assertEquals(1, pages.size)
            assertTrue(pages.single().name.endsWith(".png"))
            assertEquals("true", server.takeRequest().getHeader(AtHomeReportPolicy.SKIP_REPORT_HEADER))
            coVerify {
                mangaRepository.sendAtHomeReport(
                    match { report ->
                        report.success &&
                            !report.cached &&
                            report.bytes == "page-bytes".length &&
                            report.url.contains("/data/hash/page-1.png")
                    },
                )
            }
        }

    @Test
    fun downloadPageWithFailover_httpError_refreshesDeliveryAndRetriesOnNewServer() =
        runTest {
            val oldServer = MockWebServer()
            val newServer = MockWebServer()
            oldServer.start()
            newServer.start()
            try {
                oldServer.enqueue(MockResponse().setResponseCode(500).setBody("err"))
                newServer.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
                val refreshCount = AtomicInteger(0)
                val coordinator =
                    AtHomeFailoverCoordinator(
                        initialDelivery = delivery(oldServer, filenames = listOf("p1.png")),
                        refreshDelivery = {
                            refreshCount.incrementAndGet()
                            delivery(newServer, filenames = listOf("p1.png"))
                        },
                    )

                downloader().downloadPageWithFailover(
                    mangaId = MANGA_ID,
                    chapterId = FAILOVER_CHAPTER_ID,
                    pageIndex = 0,
                    failoverCoordinator = coordinator,
                )

                assertEquals(1, refreshCount.get())
                assertEquals(1, oldServer.requestCount)
                assertEquals(1, newServer.requestCount)
                assertEquals(1, storage.getChapterPages(MANGA_ID, FAILOVER_CHAPTER_ID).size)
            } finally {
                oldServer.shutdown()
                newServer.shutdown()
            }
        }

    @Test
    fun concurrentFailures_refreshOnceAndReportEveryAttempt() =
        runTest {
            val oldServer = MockWebServer()
            val newServer = MockWebServer()
            oldServer.start()
            newServer.start()
            try {
                val oldRequestsStarted = CountDownLatch(3)
                oldServer.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse {
                            oldRequestsStarted.countDown()
                            oldRequestsStarted.await(2, TimeUnit.SECONDS)
                            return MockResponse().setResponseCode(500).setBody("old-error")
                        }
                    }
                newServer.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            MockResponse().setResponseCode(200).setBody("new-page")
                    }
                val refreshCount = AtomicInteger(0)
                val filenames = listOf("p1.png", "p2.png", "p3.png")
                val oldBaseUrl = oldServer.url("/").toString().trimEnd('/')
                val newBaseUrl = newServer.url("/").toString().trimEnd('/')
                val coordinator =
                    AtHomeFailoverCoordinator(
                        initialDelivery = delivery(oldServer, filenames),
                        refreshDelivery = {
                            refreshCount.incrementAndGet()
                            delivery(newServer, filenames)
                        },
                    )
                val pageDownloader = downloader(ioDispatcher = Dispatchers.IO)

                filenames.indices
                    .map { pageIndex ->
                        async {
                            pageDownloader.downloadPageWithFailover(
                                mangaId = MANGA_ID,
                                chapterId = FAILOVER_CHAPTER_ID,
                                pageIndex = pageIndex,
                                failoverCoordinator = coordinator,
                            )
                        }
                    }.awaitAll()

                assertEquals(1, refreshCount.get())
                assertEquals(3, oldServer.requestCount)
                assertEquals(3, newServer.requestCount)
                coVerify(exactly = 3) {
                    mangaRepository.sendAtHomeReport(match { report -> !report.success && report.url.startsWith(oldBaseUrl) })
                }
                coVerify(exactly = 3) {
                    mangaRepository.sendAtHomeReport(match { report -> report.success && report.url.startsWith(newBaseUrl) })
                }
            } finally {
                oldServer.shutdown()
                newServer.shutdown()
            }
        }

    @Test
    fun downloadPageWithFailover_ioException_exhaustsAttemptsAndThrows() =
        runTest {
            val deadServer = MockWebServer()
            deadServer.start()
            val deadDelivery = delivery(deadServer, filenames = listOf("p1.png"))
            deadServer.shutdown()
            val coordinator =
                AtHomeFailoverCoordinator(
                    initialDelivery = deadDelivery,
                    refreshDelivery = { deadDelivery },
                )

            try {
                downloader().downloadPageWithFailover(
                    mangaId = MANGA_ID,
                    chapterId = CHAPTER_ID,
                    pageIndex = 0,
                    failoverCoordinator = coordinator,
                )
                fail("Expected IOException")
            } catch (expected: IOException) {
                assertTrue(expected.message.orEmpty().isNotBlank())
            }
        }

    @Test
    fun downloadPageWithFailover_usesContentTypeExtensionAndCacheHitReport() =
        runTest {
            server.dispatcher =
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
                            .setHeader("X-Cache", "HIT")
                            .setBody("bytes")
                    }
                }
            val filenames = listOf("p1.jpg", "p2.jpeg", "p3.png", "p4.webp", "p5.gif", "p6.bin")
            val coordinator = coordinator(delivery(server, filenames = filenames))
            val pageDownloader = downloader()

            filenames.indices.forEach { pageIndex ->
                pageDownloader.downloadPageWithFailover(
                    mangaId = MANGA_ID,
                    chapterId = EXTENSION_CHAPTER_ID,
                    pageIndex = pageIndex,
                    failoverCoordinator = coordinator,
                )
            }

            val extensions = storage.getChapterPages(MANGA_ID, EXTENSION_CHAPTER_ID).map { it.extension }
            assertEquals(listOf("jpg", "jpg", "png", "webp", "gif", "bin"), extensions)
            coVerify(atLeast = 1) {
                mangaRepository.sendAtHomeReport(match { report -> report.cached && report.success })
            }
        }

    private fun downloader(
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = UnconfinedTestDispatcher(),
    ): PageDownloader =
        PageDownloader(
            mangaRepository = mangaRepository,
            offlineDownloadStorage = storage,
            imageOkHttpClient = OkHttpClient(),
            ioDispatcher = ioDispatcher,
        )

    private fun coordinator(delivery: ChapterDelivery): AtHomeFailoverCoordinator =
        AtHomeFailoverCoordinator(
            initialDelivery = delivery,
            refreshDelivery = { delivery },
        )

    private fun delivery(
        server: MockWebServer,
        filenames: List<String>,
    ): ChapterDelivery =
        ChapterDelivery(
            baseUrl = server.url("/").toString().trimEnd('/'),
            quality = "data",
            hash = "hash",
            filenames = filenames,
        )

    private companion object {
        const val MANGA_ID = "page-downloader-manga"
        const val CHAPTER_ID = "page-downloader-chapter"
        const val FAILOVER_CHAPTER_ID = "page-downloader-failover-chapter"
        const val EXTENSION_CHAPTER_ID = "page-downloader-extension-chapter"
    }
}
