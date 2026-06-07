@file:Suppress("ktlint")

package com.example.mybookslibrary.data.remote.models

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard cho finding H7: khi MangaDex trả error-envelope (HTTP 200, thiếu baseUrl/chapter),
 * `getChapterDelivery` phải trả `Result.failure` rõ ràng thay vì NullPointerException lúc build URL.
 *
 * DTO nullable + default và repository validate result/baseUrl/chapter trước khi dựng URL.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AtHomeResponseDtoParsingTest {
    @Test
    fun getChapterDelivery_errorEnvelope_returnsFailureNotCrash() =
        runTest {
            val api = mockk<MangaDexApi>()
            val prefs = mockk<UserPreferencesDataStore>()
            coEvery { prefs.getReaderQuality() } returns "data"
            coEvery { api.getAtHomeServer(any()) } returns
                AtHomeResponseDto(
                    result = "error",
                    baseUrl = null,
                    chapter = null,
                )
            val repo = MangaRepository(api, prefs, UnconfinedTestDispatcher(testScheduler))

            val result = repo.getChapterDelivery("chapter-1")

            assertTrue("Phải là Result.failure khi server trả error-envelope", result.isFailure)
        }
}
