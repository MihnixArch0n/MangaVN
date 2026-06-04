package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.models.AtHomeChapterDto
import com.example.mybookslibrary.data.remote.models.AtHomeReportRequest
import com.example.mybookslibrary.data.remote.models.AtHomeResponseDto
import com.example.mybookslibrary.data.remote.models.ChapterListDto
import com.example.mybookslibrary.data.remote.models.MangaAttributesDto
import com.example.mybookslibrary.data.remote.models.MangaDataDto
import com.example.mybookslibrary.data.remote.models.MangaDetailResponseDto
import com.example.mybookslibrary.data.remote.models.MangaListResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Phủ phần còn lại của [MangaRepository]: discover/detail mapping, build page URL,
 * và gửi At-Home report (thành công + nuốt lỗi an toàn).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MangaRepositoryCoverageTest {
    private val api = mockk<MangaDexApi>()
    private val prefs = mockk<UserPreferencesDataStore>(relaxed = true)

    private fun repository() = MangaRepository(api, prefs, UnconfinedTestDispatcher())

    @Test
    fun getDiscoverManga_mapsDtoToDomain() =
        runTest {
            coEvery { prefs.getLanguage() } returns "en"
            coEvery { api.getMangaList(any(), any(), any()) } returns
                MangaListResponseDto(
                    data =
                        listOf(
                            MangaDataDto(
                                id = "m1",
                                attributes = MangaAttributesDto(title = mapOf("en" to "One Piece")),
                            ),
                        ),
                )

            val result = repository().getDiscoverManga().first().getOrThrow()

            assertEquals("m1", result.single().id)
            assertEquals("One Piece", result.single().title)
        }

    @Test
    fun getMangaDetail_mapsDtoToDomain() =
        runTest {
            coEvery { prefs.getLanguage() } returns "en"
            coEvery { api.getMangaDetail("m1", any()) } returns
                MangaDetailResponseDto(
                    data = MangaDataDto(id = "m1", attributes = MangaAttributesDto(title = mapOf("en" to "Bleach"))),
                )

            val manga = repository().getMangaDetail("m1").getOrThrow()

            assertEquals("Bleach", manga.title)
        }

    @Test
    fun getChapterPages_buildsUrlsFromDelivery() =
        runTest {
            coEvery { prefs.getReaderQuality() } returns "data"
            coEvery { api.getAtHomeServer("c1") } returns
                AtHomeResponseDto(
                    result = "ok",
                    baseUrl = "https://node.example",
                    chapter = AtHomeChapterDto(hash = "h1", data = listOf("p0.png", "p1.png")),
                )

            val pages = repository().getChapterPages("c1").getOrThrow()

            assertEquals(
                listOf(
                    "https://node.example/data/h1/p0.png",
                    "https://node.example/data/h1/p1.png",
                ),
                pages,
            )
        }

    @Test
    fun sendAtHomeReport_callsApi() =
        runTest {
            coEvery { api.sendAtHomeReport(any()) } returns Response.success(Unit)

            repository().sendAtHomeReport(sampleReport())

            coVerify { api.sendAtHomeReport(any()) }
        }

    @Test
    fun sendAtHomeReport_swallowsApiError() =
        runTest {
            coEvery { api.sendAtHomeReport(any()) } throws RuntimeException("network down")

            // Không được ném ra ngoài — lỗi report chỉ log, không làm vỡ luồng đọc.
            repository().sendAtHomeReport(sampleReport())

            coVerify { api.sendAtHomeReport(any()) }
        }

    @Test
    fun getChapterFeed_delegatesToMangaFeed() =
        runTest {
            coEvery { prefs.getLanguage() } returns "en"
            coEvery {
                api.getMangaFeed(any(), any(), any(), any(), any(), any(), any())
            } returns Response.success(ChapterListDto(data = emptyList(), total = 0))

            assertTrue(repository().getChapterFeed("m1").getOrThrow().isEmpty())
        }

    @Test
    fun getChapterDelivery_missingChapter_returnsFailure() =
        runTest {
            coEvery { prefs.getReaderQuality() } returns "data"
            coEvery { api.getAtHomeServer("c1") } returns
                AtHomeResponseDto(result = "ok", baseUrl = "https://node.example", chapter = null)

            assertTrue(repository().getChapterDelivery("c1").isFailure)
        }

    @Test
    fun getChapterDelivery_missingHash_returnsFailure() =
        runTest {
            coEvery { prefs.getReaderQuality() } returns "data"
            coEvery { api.getAtHomeServer("c1") } returns
                AtHomeResponseDto(
                    result = "ok",
                    baseUrl = "https://node.example",
                    chapter = AtHomeChapterDto(hash = null, data = listOf("p.png")),
                )

            assertTrue(repository().getChapterDelivery("c1").isFailure)
        }

    private fun sampleReport() =
        AtHomeReportRequest(
            url = "https://node.example/data/h1/p0.png",
            success = true,
            bytes = 1024,
            duration = 12L,
            cached = false,
        )
}
