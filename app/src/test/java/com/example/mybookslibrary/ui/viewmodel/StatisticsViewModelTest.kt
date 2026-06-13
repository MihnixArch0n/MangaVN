package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.local.dao.TopMangaCount
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chapterDao = mockk<ChapterDao>(relaxed = true)
    private val libraryDao = mockk<LibraryDao>(relaxed = true)

    private fun progress(updatedAt: Long) =
        ChapterProgressEntity(
            chapter_id = "ch-$updatedAt",
            manga_id = "manga-1",
            status = ChapterStatus.COMPLETED,
            updated_at = updatedAt,
        )

    private fun build(
        total: Int = 0,
        completed: Int = 0,
        reading: Int = 0,
        recentProgress: List<ChapterProgressEntity> = emptyList(),
        libraryItems: List<LibraryItemEntity> = emptyList(),
        topManga: List<TopMangaCount> = emptyList(),
    ): StatisticsViewModel {
        every { chapterDao.observeTotalProgressCount() } returns flowOf(total)
        every { chapterDao.observeCompletedChapterCount() } returns flowOf(completed)
        every { chapterDao.observeReadingChapterCount() } returns flowOf(reading)
        every { chapterDao.observeRecentProgress(any()) } returns flowOf(recentProgress)
        every { chapterDao.observeTopReadManga() } returns flowOf(topManga)
        every { libraryDao.observeAll() } returns flowOf(libraryItems)
        return StatisticsViewModel(chapterDao, libraryDao)
    }

    @Test
    fun uiState_gopSoLieuTongVaPhanLoaiThuVien() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val items =
                listOf(
                    LibraryItemEntity("1", "Đang đọc", "c", LibraryStatus.READING),
                    LibraryItemEntity("2", "Hoàn thành", "c", LibraryStatus.COMPLETED),
                    // Yêu thích KHÔNG loại khỏi reading/completed — cùng định nghĩa với
                    // Library filter chip; favoriteCount là metric độc lập chồng lấp.
                    LibraryItemEntity("3", "Yêu thích", "c", LibraryStatus.READING, is_favorite = true),
                )
            val top = listOf(TopMangaCount(title = "One Piece", chapterCount = 12))
            val vm = build(total = 10, completed = 4, reading = 2, libraryItems = items, topManga = top)

            vm.uiState.first { it.totalChapters == 10 }

            val state = vm.uiState.value
            assertEquals(10, state.totalChapters)
            assertEquals(4, state.completedChapters)
            assertEquals(2, state.inProgressChapters)
            assertEquals(2, state.readingCount)
            assertEquals(1, state.completedCount)
            assertEquals(1, state.favoriteCount)
            assertEquals(top, state.topManga)
        }

    @Test
    fun favoriteCount_demDocLapChongLapVoiStatus() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            // Item yêu thích duy nhất vẫn có status COMPLETED — favorite không phải status
            val items =
                listOf(
                    LibraryItemEntity("1", "Fav", "c", LibraryStatus.COMPLETED, is_favorite = true),
                )
            val vm = build(total = 1, libraryItems = items)

            vm.uiState.first { it.totalChapters == 1 }

            val state = vm.uiState.value
            assertEquals(0, state.readingCount)
            assertEquals(1, state.completedCount)
            assertEquals(1, state.favoriteCount)
        }

    @Test
    fun weeklyActivity_chapterHomNayVaoCotCuoi_chapterQua7NgayBiLoai() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val now = System.currentTimeMillis()
            val eightDaysAgo = now - TimeUnit.DAYS.toMillis(8)
            val vm = build(total = 2, recentProgress = listOf(progress(now), progress(eightDaysAgo)))

            vm.uiState.first { it.totalChapters == 2 }

            val weekly = vm.uiState.value.weeklyActivity
            assertEquals(StatisticsUiState.WEEK_DAYS, weekly.size)
            assertEquals(1, weekly.last())
            // Chapter 8 ngày trước nằm ngoài cửa sổ 7 ngày
            assertEquals(1, weekly.sum())
        }

    @Test
    fun monthlyTrend_demTheoTuan_tuanHienTaiOCotCuoi() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val now = System.currentTimeMillis()
            val tenDaysAgo = now - TimeUnit.DAYS.toMillis(10)
            val vm = build(total = 2, recentProgress = listOf(progress(now), progress(tenDaysAgo)))

            vm.uiState.first { it.totalChapters == 2 }

            val monthly = vm.uiState.value.monthlyTrend
            assertEquals(StatisticsUiState.MONTH_WEEKS, monthly.size)
            // Hôm nay → tuần hiện tại (cột cuối); 10 ngày trước → 1 tuần trước (cột kế cuối)
            assertEquals(1, monthly[StatisticsUiState.MONTH_WEEKS - 1])
            assertEquals(1, monthly[StatisticsUiState.MONTH_WEEKS - 2])
        }

    @Test
    fun uiState_macDinhRongKhiChuaCoDuLieu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val vm = build()

            vm.uiState.first { it.weeklyActivity.size == StatisticsUiState.WEEK_DAYS }

            val state = vm.uiState.value
            assertEquals(0, state.totalChapters)
            assertEquals(List(StatisticsUiState.WEEK_DAYS) { 0 }, state.weeklyActivity)
            assertEquals(List(StatisticsUiState.MONTH_WEEKS) { 0 }, state.monthlyTrend)
            assertEquals(emptyList<TopMangaCount>(), state.topManga)
        }
}
