package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun libraryItems_exposesRepositoryFlow() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val items = listOf(
            LibraryItemEntity("1", "Title", "cover", LibraryStatus.READING)
        )
        val repository = mockk<LibraryRepository>()
        every { repository.observeLibraryItems() } returns flowOf(items)
        coEvery { repository.removeBookmark(any()) } just Runs

        val viewModel = LibraryViewModel(repository, mainDispatcherRule.dispatcher)

        assertEquals(items, viewModel.libraryItems.first())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun removeBookmark_launchesRepositoryCall() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository = mockk<LibraryRepository>()
        every { repository.observeLibraryItems() } returns flowOf(emptyList())
        coEvery { repository.removeBookmark(any()) } just Runs

        val viewModel = LibraryViewModel(repository, mainDispatcherRule.dispatcher)
        viewModel.removeBookmark("manga-1")

        advanceUntilIdle()

        coVerify(exactly = 1) { repository.removeBookmark("manga-1") }
    }
}







