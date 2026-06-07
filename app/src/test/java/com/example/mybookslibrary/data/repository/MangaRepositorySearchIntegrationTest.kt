package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.NetworkModule
import com.example.mybookslibrary.domain.model.SearchFilters
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Integration test cho [MangaRepository.searchManga] và getTags qua Retrofit + [MockWebServer] thật.
 *
 * Khác với MangaRepositoryTest (mock interface MangaDexApi), test này gọi qua Retrofit thật để
 * khẳng định TÊN query param gửi lên đúng chuẩn MangaDex và response JSON parse đúng — bắt được lỗi
 * mà mock interface không thấy (vd sai tên @Query, sai mapping DTO).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MangaRepositorySearchIntegrationTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: MangaRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(
                    NetworkModule
                        .provideJson()
                        .asConverterFactory("application/json".toMediaType()),
                )
                .build()
                .create(MangaDexApi::class.java)
        val prefs = mockk<UserPreferencesDataStore>()
        coEvery { prefs.getLanguage() } returns "en"
        repository = MangaRepository(api, prefs, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun searchManga_sendsAllFilterQueryParams_andParsesResponse() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """
                    {"data":[{"id":"m1","attributes":{"title":{"en":"Naruto"},"description":{},
                    "contentRating":"safe","tags":[]},"relationships":[]}]}
                    """.trimIndent(),
                ),
            )

            val result =
                repository
                    .searchManga(
                        query = "naruto",
                        filters =
                            SearchFilters(
                                includedTagIds = listOf("tag-1"),
                                languages = listOf("vi"),
                                contentRatings = listOf("safe"),
                                statuses = listOf("ongoing"),
                            ),
                    ).first()
                    .getOrThrow()

            // Response parse đúng
            assertEquals("m1", result.single().id)
            assertEquals("Naruto", result.single().title)

            // Query param gửi lên đúng tên chuẩn MangaDex
            val request = mockWebServer.takeRequest()
            val url = requireNotNull(request.requestUrl)
            assertEquals("/manga", url.encodedPath)
            assertEquals("naruto", url.queryParameter("title"))
            assertEquals(listOf("tag-1"), url.queryParameterValues("includedTags[]"))
            assertEquals(listOf("AND"), url.queryParameterValues("includedTagsMode"))
            assertEquals(listOf("vi"), url.queryParameterValues("availableTranslatedLanguage[]"))
            assertEquals(listOf("safe"), url.queryParameterValues("contentRating[]"))
            assertEquals(listOf("ongoing"), url.queryParameterValues("status[]"))
        }

    @Test
    fun searchManga_withoutFilters_omitsEmptyListParams() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody("""{"data":[]}"""),
            )

            repository.searchManga("naruto").first().getOrThrow()

            val url = requireNotNull(mockWebServer.takeRequest().requestUrl)
            // List rỗng → Retrofit không gửi param (không lọc)
            assertEquals(emptyList<String>(), url.queryParameterValues("includedTags[]"))
            assertEquals(emptyList<String>(), url.queryParameterValues("contentRating[]"))
            assertEquals(emptyList<String>(), url.queryParameterValues("status[]"))
            assertEquals("naruto", url.queryParameter("title"))
        }

    @Test
    fun getTags_parsesIdNameGroupFromEndpoint() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """
                    {"data":[{"id":"t1","type":"tag","attributes":{"name":{"en":"Action"},"group":"genre"}}]}
                    """.trimIndent(),
                ),
            )

            val tags = repository.getTags().getOrThrow()

            assertEquals("t1", tags.single().id)
            assertEquals("Action", tags.single().name)
            assertEquals("genre", tags.single().group)
            assertEquals("/manga/tag", mockWebServer.takeRequest().requestUrl?.encodedPath)
        }
}
