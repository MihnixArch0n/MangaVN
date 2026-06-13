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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
                chapterDao.observeReadingChapterCount(),
                chapterDao.observeRecentProgress(
                    cutoff = System.currentTimeMillis() - CHART_WINDOW_MS,
                ),
            ) { total, completed, reading, recentProgress ->
                StatisticsUiState(
                    totalChapters = total,
                    completedChapters = completed,
                    inProgressChapters = reading,
                    weeklyActivity = buildWeeklyActivity(recentProgress),
                    monthlyTrend = buildMonthlyTrend(recentProgress),
                )
            }.combine(libraryDao.observeAll()) { state, libraryItems ->
                state.copy(
                    // "Đang đọc"/"Hoàn thành" cùng MỘT định nghĩa với filter chip ở Library
                    // (đếm theo status, không loại trừ yêu thích) — tránh 2 màn ra 2 số khác nhau.
                    // "Yêu thích" là metric độc lập chồng lấp 2 nhóm trên → card riêng, KHÔNG phải múi pie.
                    readingCount = libraryItems.count { it.status == LibraryStatus.READING },
                    completedCount = libraryItems.count { it.status == LibraryStatus.COMPLETED },
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
            private const val CHART_WINDOW_MS = 28L * 24 * 60 * 60 * 1000
        }
    }

private fun buildWeeklyActivity(progress: List<ChapterProgressEntity>): List<Int> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val result = IntArray(StatisticsUiState.WEEK_DAYS)
    progress.forEach { chapter ->
        val chapterDate = Instant.ofEpochMilli(chapter.updated_at).atZone(zone).toLocalDate()
        val daysAgo = ChronoUnit.DAYS.between(chapterDate, today).toInt()
        if (daysAgo in 0 until StatisticsUiState.WEEK_DAYS) {
            result[StatisticsUiState.WEEK_DAYS - 1 - daysAgo]++
        }
    }
    return result.toList()
}

private fun buildMonthlyTrend(progress: List<ChapterProgressEntity>): List<Int> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val result = IntArray(StatisticsUiState.MONTH_WEEKS)
    progress.forEach { chapter ->
        val chapterDate = Instant.ofEpochMilli(chapter.updated_at).atZone(zone).toLocalDate()
        val daysAgo = ChronoUnit.DAYS.between(chapterDate, today).toInt()
        val weeksAgo = daysAgo / DAYS_PER_WEEK
        if (weeksAgo in 0 until StatisticsUiState.MONTH_WEEKS) {
            result[StatisticsUiState.MONTH_WEEKS - 1 - weeksAgo]++
        }
    }
    return result.toList()
}

private const val DAYS_PER_WEEK = 7
