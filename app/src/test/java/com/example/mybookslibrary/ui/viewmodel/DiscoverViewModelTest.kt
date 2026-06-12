package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val application = mockk<Application>()
    private val repository = mockk<MangaRepository>()
    private val libraryDao = mockk<LibraryDao> {
        every { observeRecentlyReading() } returns flowOf(emptyList())
    }

    private fun viewModel() =
        DiscoverViewModel(
            application = application,
            repository = repository,
            libraryDao = libraryDao,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )

    @Test
    fun init_taiThanhCong_capNhatItems() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manga = MangaModel("m1", "Title", "Desc", null, listOf("tag"))
            every { repository.getDiscoverManga(20, 0) } returns flowOf(Result.success(listOf(manga)))

            val vm = viewModel()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertEquals(listOf(manga), vm.uiState.value.items)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun init_loiCoMessage_dungMessage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { repository.getDiscoverManga(20, 0) } returns
                flowOf(Result.failure(IllegalStateException("mạng lỗi")))

            val vm = viewModel()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertEquals("mạng lỗi", vm.uiState.value.error)
        }

    @Test
    fun init_loiNullMessage_dungStringRes() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { application.getString(R.string.error_load_discover) } returns "Không tải được"
            every { repository.getDiscoverManga(20, 0) } returns flowOf(Result.failure(RuntimeException()))

            val vm = viewModel()
            advanceUntilIdle()

            assertEquals("Không tải được", vm.uiState.value.error)
        }

    @Test
    fun loadDiscover_truyenDungLimitOffset() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { repository.getDiscoverManga(any(), any()) } returns flowOf(Result.success(emptyList()))

            val vm = viewModel()
            advanceUntilIdle()
            vm.loadDiscover(limit = 5, offset = 10)
            advanceUntilIdle()

            verify { repository.getDiscoverManga(5, 10) }
        }
}
