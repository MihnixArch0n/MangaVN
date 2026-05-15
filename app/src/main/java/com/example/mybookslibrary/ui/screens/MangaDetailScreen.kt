package com.example.mybookslibrary.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import com.example.mybookslibrary.ui.util.appString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.viewmodel.MangaDetailViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

// Minimal no-op shared bounds helper. The real shared transition implementation
// is provided via CompositionLocal in `MainNavGraph` when enabled. This stub
// keeps this file compilable when the transition is not used.
fun Modifier.sharedCoverBounds(mangaId: String): Modifier = composed { this@composed }

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
    onReviewClick: (mangaId: String) -> Unit = {},
    viewModel: MangaDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    var chaptersExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // Ảnh nền mờ
            item {
                Box(modifier = Modifier.fillMaxWidth().height(DetailDimensions.BackdropHeight)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .placeholderMemoryCacheKey("cover_$mangaId")
                            .memoryCacheKey("cover_$mangaId")
                            .build(),
                        contentDescription = null,
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
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .placeholderMemoryCacheKey("cover_$mangaId")
                                .memoryCacheKey("cover_$mangaId")
                                .build(),
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
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

            // Từ nhà xuất bản (From the Publisher)
            if (displayDescription.isNotBlank()) {
                item {
                    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                    Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.SynopsisOffset)) {
                        Text(
                            "From the Publisher",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.animateContentSize().clickable { expanded = !expanded }) {
                            Text(
                                displayDescription,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (expanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!expanded) {
                            Text(
                                "More",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp).clickable { expanded = true }
                            )
                        }
                    }
                }
            }

            // From the Book
            if (uiState.isLoadingFirstChapterPages) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (uiState.firstChapterPages.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(32.dp).offset(y = DetailDimensions.SynopsisOffset))
                    Column(modifier = Modifier.fillMaxWidth().offset(y = DetailDimensions.SynopsisOffset)) {
                        Text(
                            "From the Book",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.firstChapterPages) { pageUrl ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier.width(200.dp).height(300.dp)
                                ) {
                                    AsyncImage(
                                        model = pageUrl,
                                        contentDescription = "Page Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Reviews
            item {
                Spacer(Modifier.height(40.dp).offset(y = DetailDimensions.SynopsisOffset))
                Column(modifier = Modifier.fillMaxWidth().offset(y = DetailDimensions.SynopsisOffset)) {
                    Text(
                        "Customer Reviews >",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .clickable { onReviewClick(mangaId) }
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val dummyReviews = listOf(
                            DummyReview("Great read", "I couldn't put this down. The story is engaging and the art is fantastic.", "Oct 12, 2025", "User123"),
                            DummyReview("A masterpiece", "Truly one of the best mangas I've read in a long time. Highly recommend it to anyone.", "Nov 05, 2025", "MangaFan99"),
                            DummyReview("Stunning visuals", "The attention to detail in every panel is just breathtaking.", "Dec 20, 2025", "ArtLover")
                        )
                        items(dummyReviews) { review ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillParentMaxWidth(0.85f)
                                    .clickable { onReviewClick(mangaId) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(review.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Spacer(Modifier.weight(1f))
                                        Text(review.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row {
                                        repeat(5) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(review.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(8.dp))
                                    Text(review.username, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
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
                Spacer(Modifier.height(40.dp).offset(y = DetailDimensions.ChaptersOffset))
                Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.ChaptersOffset)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { chaptersExpanded = !chaptersExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(appString(R.string.detail_chapters), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                        Icon(
                            imageVector = if (chaptersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand Chapters"
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (chaptersExpanded) {
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

// Chapter components are moved to `ChapterComponents.kt` to keep this file small and modular.
// Use the canonical implementations from `ChapterComponents.kt` (VolumeHeader, ChapterRow, buildChapterTitle).

// Minimal dummy review model used for preview lists:
data class DummyReview(
    val title: String,
    val body: String,
    val date: String,
    val username: String
)
