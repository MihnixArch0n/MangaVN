package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import com.example.mybookslibrary.ui.util.appString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.ui.viewmodel.MangaDetailViewModel

private object DetailDimensions {
    val BackdropHeight = 280.dp
    val CoverWidth = 160.dp
    val CoverHeight = 240.dp
    val CoverRowOffset = (-80).dp
    val ActionOffset = (-60).dp
    val SynopsisOffset = (-40).dp
    val ChaptersOffset = (-20).dp
    val BlurRadius = 20.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaDetailScreen(
    mangaId: String,
    title: String,
    coverArt: String,
    description: String,
    tags: List<String>,
    onBackClick: () -> Unit,
    onReadChapter: (mangaId: String, chapterId: String, chapterTitle: String) -> Unit,
    viewModel: MangaDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.mangaDetail
    val displayTitle = title.ifBlank { detail?.title ?: "" }
    val displayDescription = description.ifBlank { detail?.description ?: "" }
    val displayTags = tags.ifEmpty { detail?.tags ?: emptyList() }
    val displayCoverArt = coverArt.ifBlank { detail?.coverArt ?: "" }
    val coverUrl = displayCoverArt.ifBlank { null }
    val noVolumeLabel = appString(R.string.chapter_no_volume)
    val groupedChapters = remember(uiState.chapters, noVolumeLabel) {
        uiState.chapters.groupBy { chapter ->
            chapter.volume?.takeIf { it.isNotBlank() } ?: noVolumeLabel
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // Ảnh nền mờ
            item {
                Box(modifier = Modifier.fillMaxWidth().height(DetailDimensions.BackdropHeight)) {
                    AsyncImage(
                        model = coverUrl, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(radius = DetailDimensions.BlurRadius)
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.background
                                ), startY = 120f
                            )
                        )
                    )
                }
            }

            // Bìa + tiêu đề
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        .offset(y = DetailDimensions.CoverRowOffset),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Card(
                        modifier = Modifier.size(DetailDimensions.CoverWidth, DetailDimensions.CoverHeight),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        AsyncImage(model = coverUrl, contentDescription = displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Column(modifier = Modifier.padding(start = 20.dp, bottom = 8.dp).weight(1f)) {
                        if (displayTags.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                displayTags.take(2).forEach { tag ->
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(displayTitle, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary,
                            maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Nút hành động
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.ActionOffset)) {
                    val firstChapter = uiState.chapters.firstOrNull()
                    val firstChapterTitle = firstChapter?.let { buildChapterTitle(it) } ?: ""
                    Button(
                        onClick = {
                            if (firstChapter != null) {
                                viewModel.ensureInLibrary(displayTitle, displayCoverArt)
                                onReadChapter(mangaId, firstChapter.chapterId, firstChapterTitle)
                            }
                        },
                        enabled = firstChapter != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            if (firstChapter != null) appString(R.string.detail_read_now) else appString(R.string.detail_loading),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.toggleLibrary(displayTitle, displayCoverArt) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp,
                            if (uiState.isInLibrary) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (uiState.isInLibrary) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            if (uiState.isInLibrary) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isInLibrary) appString(R.string.detail_in_library) else appString(R.string.detail_add_to_library),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Tóm tắt — hiện nguyên bản từ MangaDex
            if (displayDescription.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.SynopsisOffset)) {
                        Text(appString(R.string.detail_synopsis), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(displayDescription, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Lỗi tải chi tiết
            if (uiState.detailError != null && detail == null) {
                item {
                    Text(appString(R.string.detail_error), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }

            // Danh sách chương
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.ChaptersOffset)) {
                    Spacer(Modifier.height(16.dp))
                    Text(appString(R.string.detail_chapters), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                }
            }

            when {
                uiState.isLoadingChapters -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                uiState.chaptersError != null -> {
                    item {
                        Text(appString(R.string.detail_chapters_error), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
                uiState.chapters.isEmpty() -> {
                    item {
                        Text(appString(R.string.detail_chapters_empty), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
                else -> {
                    groupedChapters.forEach { (volume, chapters) ->
                        item(key = "header-$volume") { VolumeHeader(volume) }
                        items(chapters, key = { it.chapterId }) { chapter ->
                            val chTitle = buildChapterTitle(chapter)
                            ChapterRow(
                                chapter = chapter,
                                chapterTitle = chTitle,
                                onClick = {
                                    viewModel.ensureInLibrary(displayTitle, displayCoverArt)
                                    onReadChapter(mangaId, chapter.chapterId, chTitle)
                                },
                                onMarkCompleted = { viewModel.markChapterCompleted(chapter.chapterId, chapter.totalPages) },
                                onMarkUnread = { viewModel.markChapterUnread(chapter.chapterId, chapter.totalPages) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        // Nút quay lại
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, appString(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun VolumeHeader(volume: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(
            text = volume,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterWithProgressModel,
    chapterTitle: String,
    onClick: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkUnread: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCompleted = chapter.status == ChapterReadingStatus.COMPLETED
    val contentAlpha = if (isCompleted) 0.55f else 1f

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapterTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!chapter.title.isNullOrBlank()) {
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when (chapter.status) {
                    ChapterReadingStatus.UNREAD -> Text(
                        appString(R.string.status_unread),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                    ChapterReadingStatus.READING -> Text(
                        text = "${appString(R.string.status_reading)} · Page ${chapter.lastReadPage + 1}/${chapter.totalPages.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    ChapterReadingStatus.COMPLETED -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            appString(R.string.status_completed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                appString(R.string.detail_pages_suffix, chapter.totalPages),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            if (chapter.status != ChapterReadingStatus.COMPLETED) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.chapter_mark_completed)) },
                    onClick = {
                        menuExpanded = false
                        onMarkCompleted()
                    }
                )
            }
            if (chapter.status != ChapterReadingStatus.UNREAD) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.chapter_mark_unread)) },
                    onClick = {
                        menuExpanded = false
                        onMarkUnread()
                    }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    )
}

@Composable
private fun buildChapterTitle(ch: ChapterWithProgressModel): String {
    val vol = if (!ch.volume.isNullOrBlank()) appString(R.string.chapter_vol_prefix, ch.volume) else ""
    val num = if (!ch.chapterNumber.isNullOrBlank()) appString(R.string.chapter_num_prefix, ch.chapterNumber) else appString(R.string.chapter_oneshot)
    return "$vol$num"
}
