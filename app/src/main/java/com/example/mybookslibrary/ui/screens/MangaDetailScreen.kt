package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ChapterModel
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
                                onReadChapter(mangaId, firstChapter.id, firstChapterTitle)
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
                    items(uiState.chapters, key = { it.id }) { chapter ->
                        val chTitle = buildChapterTitle(chapter)
                        ChapterRow(chapter, chTitle) {
                            viewModel.ensureInLibrary(displayTitle, displayCoverArt)
                            onReadChapter(mangaId, chapter.id, chTitle)
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
private fun ChapterRow(chapter: ChapterModel, chapterTitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(chapterTitle, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!chapter.title.isNullOrBlank()) {
                Text(chapter.title, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(appString(R.string.detail_pages_suffix, chapter.pages), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 24.dp)
        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)))
}

@Composable
private fun buildChapterTitle(ch: ChapterModel): String {
    val vol = if (!ch.volume.isNullOrBlank()) appString(R.string.chapter_vol_prefix, ch.volume!!) else ""
    val num = if (!ch.chapter.isNullOrBlank()) appString(R.string.chapter_num_prefix, ch.chapter!!) else appString(R.string.chapter_oneshot)
    return "$vol$num"
}
