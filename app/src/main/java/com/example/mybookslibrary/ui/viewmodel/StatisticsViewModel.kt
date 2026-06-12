package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.local.dao.TopMangaCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class StatisticsUiState(
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val inProgressChapters: Int = 0,
    val weeklyActivity: List<Int> = List(WEEK_DAYS) { 0 },
    val monthlyTrend: List<Int> = List(MONTH_WEEKS) { 0 },
    val readingCount: Int = 0,
    val completedCount: Int = 0,
    val favoriteCount: Int = 0,
    val topManga: List<TopMangaCount> = emptyList(),
) {
    companion object {
        const val WEEK_DAYS = 7
        const val MONTH_WEEKS = 4
    }
}

@HiltViewModel
class StatisticsViewModel
    @Inject
    constructor(
        chapterDao: ChapterDao,
        libraryDao: LibraryDao,
    ) : ViewModel() {
        val uiState: StateFlow<StatisticsUiState> =
            combine(
                chapterDao.observeTotalProgressCount(),
                chapterDao.observeCompletedChapterCount(),
                chapterDao.observeRecentProgress(),
                libraryDao.observeAll(),
            ) { total, completed, recentProgress, libraryItems ->
                StatisticsUiState(
                    totalChapters = total,
                    completedChapters = completed,
                    inProgressChapters = total - completed,
                    weeklyActivity = buildWeeklyActivity(recentProgress),
                    monthlyTrend = buildMonthlyTrend(recentProgress),
                    readingCount = libraryItems.count { it.status == LibraryStatus.READING },
                    completedCount = libraryItems.count { it.status == LibraryStatus.COMPLETED },
                    // Yêu thích là cờ độc lập (is_favorite), không phải một status
                    favoriteCount = libraryItems.count { it.is_favorite },
                )
            }.combine(chapterDao.observeTopReadManga()) { state, topManga ->
                state.copy(topManga = topManga)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
                StatisticsUiState(),
            )

        companion object {
            private const val SUBSCRIPTION_TIMEOUT = 5_000L
        }
    }

private fun buildWeeklyActivity(progress: List<ChapterProgressEntity>): List<Int> {
    val now = System.currentTimeMillis()
    val result = IntArray(StatisticsUiState.WEEK_DAYS)
    progress.forEach { chapter ->
        val daysAgo = TimeUnit.MILLISECONDS.toDays(now - chapter.updated_at).toInt()
        if (daysAgo in 0 until StatisticsUiState.WEEK_DAYS) {
            result[StatisticsUiState.WEEK_DAYS - 1 - daysAgo]++
        }
    }
    return result.toList()
}

private fun buildMonthlyTrend(progress: List<ChapterProgressEntity>): List<Int> {
    val now = System.currentTimeMillis()
    val result = IntArray(StatisticsUiState.MONTH_WEEKS)
    progress.forEach { chapter ->
        val weeksAgo = (TimeUnit.MILLISECONDS.toDays(now - chapter.updated_at) / DAYS_PER_WEEK).toInt()
        if (weeksAgo in 0 until StatisticsUiState.MONTH_WEEKS) {
            result[StatisticsUiState.MONTH_WEEKS - 1 - weeksAgo]++
        }
    }
    return result.toList()
}

private const val DAYS_PER_WEEK = 7
