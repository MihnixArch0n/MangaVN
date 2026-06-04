package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.model.MangaTag
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // getTags được gọi trong init → mọi test phải stub; mặc định trả rỗng.
    private fun repositoryWithEmptyTags(): MangaRepository {
        val repository = mockk<MangaRepository>()
        coEvery { repository.getTags() } returns Result.success(emptyList())
        return repository
    }

    @Test
    fun onQueryChange_clearsResultsForShortQuery() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = repositoryWithEmptyTags()
            every { repository.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))

            val viewModel = SearchViewModel(repository)
            viewModel.onQueryChange("a")

            assertEquals("a", viewModel.uiState.value.query)
            assertTrue(
                viewModel.uiState.value.results
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun searchUpdatesResultsAfterDebounce() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manga = MangaModel("m1", "Title", "Desc", null, listOf("tag"))
            val repository = repositoryWithEmptyTags()
            every { repository.searchManga("naruto", any()) } returns flowOf(Result.success(listOf(manga)))

            val viewModel = SearchViewModel(repository)
            viewModel.onQueryChange("naruto")

            advanceTimeBy(400)
            advanceUntilIdle()

            assertEquals("naruto", viewModel.uiState.value.query)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(listOf(manga), viewModel.uiState.value.results)
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun init_loadsTagsIntoGenreAndThemeBuckets() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = mockk<MangaRepository>()
            every { repository.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))
            coEvery { repository.getTags() } returns
                Result.success(
                    listOf(
                        MangaTag("g1", "Action", "genre"),
                        MangaTag("t1", "Magic", "theme"),
                        // Tag nhóm format phải bị loại khỏi cả hai bucket genre/theme.
                        MangaTag("f1", "Oneshot", "format"),
                    ),
                )

            val viewModel = SearchViewModel(repository)
            advanceUntilIdle()

            assertEquals(
                listOf("Action"),
                viewModel.uiState.value.availableGenres
                    .map { it.name },
            )
            assertEquals(
                listOf("Magic"),
                viewModel.uiState.value.availableThemes
                    .map { it.name },
            )
        }

    @Test
    fun onToggleTag_searchesWithIncludedTagAndUpdatesResults() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manga = MangaModel("m1", "Title", "Desc", null, listOf("tag"))
            val repository = repositoryWithEmptyTags()
            every {
                repository.searchManga(any(), match { it.includedTagIds.contains("g1") })
            } returns flowOf(Result.success(listOf(manga)))

            val viewModel = SearchViewModel(repository)
            viewModel.onToggleTag("g1")
            advanceTimeBy(400)
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.selectedTagIds
                    .contains("g1"),
            )
            assertEquals(1, viewModel.uiState.value.activeFilterCount)
            assertEquals(listOf(manga), viewModel.uiState.value.results)
        }

    @Test
    fun onClearFilters_resetsSelectionsAndCount() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = repositoryWithEmptyTags()
            every { repository.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))

            val viewModel = SearchViewModel(repository)
            viewModel.onToggleLanguage("vi")
            assertEquals(1, viewModel.uiState.value.activeFilterCount)

            viewModel.onClearFilters()

            assertEquals(0, viewModel.uiState.value.activeFilterCount)
            assertTrue(
                viewModel.uiState.value.selectedLanguages
                    .isEmpty(),
            )
        }

    // Guard regression: rút query về <2 ký tự khi search đang chạy thì kết quả cũ KHÔNG được
    // ghi đè state đã xoá (flatMapLatest phải huỷ search trước). Dùng flow có delay để mô phỏng
    // search còn treo — flowOf emit tức thì không tái hiện được race này.
    @Test
    fun staleResultDoesNotOverwriteAfterQueryShortened() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manga = MangaModel("m1", "Naruto", "Desc", null, listOf("tag"))
            val repository = repositoryWithEmptyTags()
            every { repository.searchManga("naruto", any()) } returns
                flow {
                    delay(1000)
                    emit(Result.success(listOf(manga)))
                }

            val viewModel = SearchViewModel(repository)
            viewModel.onQueryChange("naruto")
            advanceTimeBy(500) // qua debounce, search đang treo (chưa emit)
            viewModel.onQueryChange("n") // rút ngắn → nhánh idle, phải huỷ search cũ
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.results
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }
}
