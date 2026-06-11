package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.MangaDetailViewModel
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@Composable
fun MangaDetailScreen(
    mangaId: String,
    onBackClick: () -> Unit,
    onReadChapter: (mangaId: String, chapterId: String, chapterTitle: String, startPageIndex: Int) -> Unit,
    onReviewClick: (mangaId: String) -> Unit = {},
    onShareClick: ((mangaTitle: String) -> Unit)? = null,
    viewModel: MangaDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = uiState.mangaDetail
    val displayTitle = detail?.title.orEmpty()
    val displayDescription = detail?.description.orEmpty()
    val displayTags = detail?.tags.orEmpty()
    val displayCoverArt = detail?.coverArt.orEmpty()
    val coverUrl = displayCoverArt.ifBlank { null }
    val noVolumeLabel = appString(R.string.chapter_no_volume)
    val groupedChapters =
        remember(uiState.chapters, noVolumeLabel) {
            uiState.chapters.groupBy { chapter ->
                chapter.volume?.takeIf { it.isNotBlank() } ?: noVolumeLabel
            }
        }
    var chaptersExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                MangaDetailBackdrop(
                    mangaId = mangaId,
                    coverUrl = coverUrl,
                )
            }
            item {
                MangaDetailHeader(
                    mangaId = mangaId,
                    title = displayTitle,
                    coverUrl = coverUrl,
                    tags = displayTags,
                )
            }
            item {
                val lastReadChapter = uiState.lastReadChapterId?.let { id -> uiState.chapters.find { it.chapterId == id } }
                val readingChapter = uiState.chapters.find { it.status == ChapterReadingStatus.READING }
                val firstUnreadChapter = uiState.chapters.find { it.status == ChapterReadingStatus.UNREAD }
                val firstChapter = uiState.chapters.firstOrNull()
                
                val targetChapter = lastReadChapter ?: readingChapter ?: firstUnreadChapter ?: firstChapter
                val targetChapterTitle = targetChapter?.let { buildChapterTitle(it) }.orEmpty()
                MangaDetailActions(
                    isInLibrary = uiState.isInLibrary,
                    firstChapter = targetChapter,
                    onReadNow = { chapter ->
                        val startPageIndex = chapter.resumePageIndex()
                        Timber.d(
                            "MangaDetail read-now/continue: mangaId=%s chapterId=%s status=%s lastReadPage=%d " +
                                "startPageIndex=%d totalPages=%d",
                            mangaId,
                            chapter.chapterId,
                            chapter.status,
                            chapter.lastReadPage,
                            startPageIndex,
                            chapter.totalPages,
                        )
                        viewModel.ensureInLibrary(displayTitle, displayCoverArt)
                        onReadChapter(mangaId, chapter.chapterId, targetChapterTitle, startPageIndex)
                    },
                    onToggleLibrary = {
                        viewModel.toggleLibrary(displayTitle, displayCoverArt)
                    },
                )
            }
            if (displayDescription.isNotBlank()) {
                item {
                    PublisherSection(description = displayDescription)
                }
            }
            if (uiState.isLoadingFirstChapterPages) {
                item {
                    Box(Modifier.padding(32.dp).fillParentMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (uiState.firstChapterPages.isNotEmpty()) {
                item {
                    FirstChapterPreviewSection(pageUrls = uiState.firstChapterPages)
                }
            }
            item {
                CustomerReviewsSection(onReviewClick = { onReviewClick(mangaId) })
            }
            if (uiState.detailError != null && detail == null) {
                item {
                    Text(
                        appString(R.string.detail_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
            item {
                Spacer(Modifier.height(40.dp).offset(y = DetailDimensions.ChaptersOffset))
                ChaptersHeader(
                    expanded = chaptersExpanded,
                    modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .offset(y = DetailDimensions.ChaptersOffset)
                        .clickable { chaptersExpanded = !chaptersExpanded },
                )
            }
            if (chaptersExpanded) {
                if (uiState.availableLanguages.isNotEmpty()) {
                    item {
                        LanguageFilterRow(
                            availableLanguages = uiState.availableLanguages,
                            selectedLanguage = uiState.selectedLanguage,
                            onLanguageSelected = viewModel::selectLanguage,
                            modifier = Modifier.padding(bottom = 8.dp).offset(y = DetailDimensions.ChaptersOffset)
                        )
                    }
                }
                when {
                    uiState.isLoadingChapters -> {
                        item {
                            Box(Modifier.padding(32.dp).fillParentMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    uiState.chaptersError != null -> {
                        item { DetailMessage(appString(R.string.detail_chapters_error)) }
                    }
                    uiState.chapters.isEmpty() -> {
                        item { DetailMessage(appString(R.string.detail_chapters_empty)) }
                    }
                    else -> {
                        groupedChapters.forEach { (volume, chapters) ->
                            item(key = "header-$volume") { VolumeHeader(volume) }
                            items(chapters, key = { it.chapterId }) { chapter ->
                                val chapterTitle = buildChapterTitle(chapter)
                                ChapterRow(
                                    chapter = chapter,
                                    chapterTitle = chapterTitle,
                                    onClick = {
                                        val startPageIndex = chapter.resumePageIndex()
                                        Timber.d(
                                            "MangaDetail chapter click: mangaId=%s chapterId=%s status=%s " +
                                                "lastReadPage=%d startPageIndex=%d totalPages=%d",
                                            mangaId,
                                            chapter.chapterId,
                                            chapter.status,
                                            chapter.lastReadPage,
                                            startPageIndex,
                                            chapter.totalPages,
                                        )
                                        viewModel.ensureInLibrary(displayTitle, displayCoverArt)
                                        onReadChapter(mangaId, chapter.chapterId, chapterTitle, startPageIndex)
                                    },
                                    onMarkCompleted = {
                                        viewModel.markChapterCompleted(chapter.chapterId, chapter.totalPages)
                                    },
                                    onMarkUnread = {
                                        viewModel.markChapterUnread(chapter.chapterId, chapter.totalPages)
                                    },
                                    onStartDownload = {
                                        viewModel.startChapterDownload(chapter.chapterId)
                                    },
                                    onCancelDownload = {
                                        viewModel.cancelChapterDownload(chapter.chapterId)
                                    },
                                    onDeleteDownload = {
                                        viewModel.deleteChapterDownload(chapter.chapterId)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }

        DetailBackButton(
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (onShareClick != null && detail != null) {
            DetailShareButton(
                onShareClick = { onShareClick(displayTitle) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

private fun ChapterWithProgressModel.resumePageIndex(): Int {
    val rawPageIndex = if (status == ChapterReadingStatus.UNREAD) 0 else lastReadPage
    return if (totalPages > 0) rawPageIndex.coerceIn(0, totalPages - 1) else 0
}

@Composable
fun LanguageFilterRow(
    availableLanguages: List<String>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableLanguages.isEmpty()) return
    LazyRow(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedLanguage == "",
                onClick = { onLanguageSelected("") },
                label = { Text("All") }
            )
        }
        items(availableLanguages) { lang ->
            FilterChip(
                selected = selectedLanguage == lang,
                onClick = { onLanguageSelected(lang) },
                label = { Text(lang.uppercase()) }
            )
        }
    }
}
