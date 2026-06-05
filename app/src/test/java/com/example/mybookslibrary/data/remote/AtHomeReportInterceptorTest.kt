package com.example.mybookslibrary.data.remote

import com.example.mybookslibrary.data.repository.MangaRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AtHomeReportInterceptorTest {

    @Test
    fun success_reportsBytesDurationAndCacheState() = runTest {
        val repository = mockk<MangaRepository>(relaxed = true)
        val scope = TestScope(testScheduler)
        val body = "abcdef"
        val client = client(
            repository = repository,
            scope = scope,
            terminalInterceptor = Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("X-Cache", "HIT from-node")
                    .body(body.toResponseBody("image/png".toMediaType()))
                    .build()
            }
        )

        val response = client.newCall(request("https://node.example.net/data/hash/page.png")).execute()

        assertEquals(body, response.body.string())
        scope.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.sendAtHomeReport(
                match { report ->
                    report.url == "https://node.example.net/data/hash/page.png" &&
                        report.success &&
                        report.bytes == body.length &&
                        report.duration >= 0L &&
                        report.cached
                }
            )
        }
    }

    @Test
    fun networkFailure_reportsFailureWithZeroBytes() = runTest {
        val repository = mockk<MangaRepository>(relaxed = true)
        val scope = TestScope(testScheduler)
        val client = client(
            repository = repository,
            scope = scope,
            terminalInterceptor = Interceptor {
                throw IOException("boom")
            }
        )

        try {
            client.newCall(request("https://node.example.net/data/hash/page.png")).execute()
            fail("Expected IOException")
        } catch (_: IOException) {
            // Expected.
        }
        scope.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.sendAtHomeReport(
                match { report ->
                    report.url == "https://node.example.net/data/hash/page.png" &&
                        !report.success &&
                        report.bytes == 0 &&
                        report.duration >= 0L &&
                        !report.cached
                }
            )
        }
    }

    @Test
    fun mangadexOrgImage_isSkipped() = runTest {
        val repository = mockk<MangaRepository>(relaxed = true)
        val scope = TestScope(testScheduler)
        val client = client(
            repository = repository,
            scope = scope,
            terminalInterceptor = Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("image".toResponseBody("image/png".toMediaType()))
                    .build()
            }
        )

        client.newCall(request("https://uploads.mangadex.org/data/hash/page.png")).execute().use { response ->
            response.body.string()
        }
        scope.advanceUntilIdle()

        coVerify(exactly = 0) { repository.sendAtHomeReport(any()) }
    }

    @Test
    fun skipHeader_goBoHeaderVaKhongReport() = runTest {
        val repository = mockk<MangaRepository>(relaxed = true)
        val scope = TestScope(testScheduler)
        var seenSkipHeader: String? = "unset"
        val client = client(
            repository = repository,
            scope = scope,
            terminalInterceptor = Interceptor { chain ->
                // Header skip phải bị gỡ trước khi đi tiếp
                seenSkipHeader = chain.request().header(AtHomeReportPolicy.SKIP_REPORT_HEADER)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("img".toResponseBody("image/png".toMediaType()))
                    .build()
            }
        )

        val request = Request.Builder()
            .url("https://node.example.net/data/hash/page.png")
            .header(AtHomeReportPolicy.SKIP_REPORT_HEADER, "true")
            .build()
        client.newCall(request).execute().use { it.body.string() }
        scope.advanceUntilIdle()

        assertEquals(null, seenSkipHeader)
        coVerify(exactly = 0) { repository.sendAtHomeReport(any()) }
    }

    @Test
    fun success_khongCoXCache_reportCachedFalse() = runTest {
        val repository = mockk<MangaRepository>(relaxed = true)
        val scope = TestScope(testScheduler)
        val client = client(
            repository = repository,
            scope = scope,
            terminalInterceptor = Interceptor { chain ->
                // Không header X-Cache -> cached = false (nhánh ?.startsWith == true là false)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("abc".toResponseBody("image/png".toMediaType()))
                    .build()
            }
        )

        client.newCall(request("https://node.example.net/data/hash/page.png")).execute().use {
            // contentLength() và source() — cover cả 2 override trong custom ResponseBody
            it.body.contentLength()
            it.body.string()
        }
        scope.advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.sendAtHomeReport(match { report -> !report.cached && report.success })
        }
    }

    private fun client(
        repository: MangaRepository,
        scope: TestScope,
        terminalInterceptor: Interceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(AtHomeReportInterceptor(repository, scope))
            .addInterceptor(terminalInterceptor)
            .build()

    private fun request(url: String): Request =
        Request
            .Builder()
            .url(url)
            .build()
}
