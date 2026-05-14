package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onQueryChange_clearsResultsForShortQuery() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository = mockk<MangaRepository>()
        every { repository.searchManga(any()) } returns flowOf(Result.success(emptyList()))

        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChange("a")

        assertEquals("a", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun searchUpdatesResultsAfterDebounce() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val manga = MangaModel("m1", "Title", "Desc", null, listOf("tag"))
        val repository = mockk<MangaRepository>()
        every { repository.searchManga("naruto") } returns flowOf(Result.success(listOf(manga)))

        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChange("naruto")

        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals("naruto", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(manga), viewModel.uiState.value.results)
        assertEquals(null, viewModel.uiState.value.error)
    }
}




