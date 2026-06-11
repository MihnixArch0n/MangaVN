package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.ui.util.appString

@Composable
internal fun MangaDetailBackdrop(mangaId: String, coverUrl: String?,) {
    Box(modifier = Modifier.fillMaxWidth().height(DetailDimensions.BackdropHeight)) {
        AsyncImage(
            model = coverRequest(mangaId, coverUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(radius = DetailDimensions.BlurRadius),
        )
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background,
                        ),
                        startY = 120f,
                    ),
                ),
        )
    }
}

@Composable
internal fun MangaDetailHeader(mangaId: String, title: String, coverUrl: String?, tags: List<String>,) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = DetailDimensions.CoverRowOffset),
        verticalAlignment = Alignment.Bottom,
    ) {
        Card(
            modifier = Modifier.size(DetailDimensions.CoverWidth, DetailDimensions.CoverHeight),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            AsyncImage(
                model = coverRequest(mangaId, coverUrl),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.padding(start = 20.dp, bottom = 8.dp).weight(1f)) {
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.take(2).forEach { tag ->
                        Box(
                            modifier =
                            Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun MangaDetailActions(
    isInLibrary: Boolean,
    firstChapter: ChapterWithProgressModel?,
    onReadNow: (ChapterWithProgressModel) -> Unit,
    onToggleLibrary: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.ActionOffset)) {
        Button(
            onClick = { firstChapter?.let(onReadNow) },
            enabled = firstChapter != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                if (firstChapter != null) {
                    if (
                        firstChapter.status == com.example.mybookslibrary.domain.model.ChapterReadingStatus.READING ||
                        firstChapter.status == com.example.mybookslibrary.domain.model.ChapterReadingStatus.COMPLETED
                    ) {
                        appString(R.string.detail_continue_reading)
                    } else {
                        appString(R.string.detail_read_now)
                    }
                } else {
                    appString(R.string.detail_loading)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onToggleLibrary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border =
            BorderStroke(
                1.dp,
                if (isInLibrary) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
            ),
            colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor =
                if (isInLibrary) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
        ) {
            Icon(
                if (isInLibrary) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isInLibrary) appString(R.string.detail_in_library) else appString(R.string.detail_add_to_library),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun DetailBackButton(onBackClick: () -> Unit, modifier: Modifier = Modifier,) {
    IconButton(
        onClick = onBackClick,
        modifier = modifier.statusBarsPadding().padding(8.dp),
    ) {
        Box(
            modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                appString(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun DetailShareButton(onShareClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onShareClick,
        modifier = modifier.statusBarsPadding().padding(8.dp),
    ) {
        Box(
            modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = "Share manga",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun coverRequest(mangaId: String, coverUrl: String?,): ImageRequest = ImageRequest
    .Builder(LocalContext.current)
    .data(coverUrl)
    .placeholderMemoryCacheKey("cover_$mangaId")
    .memoryCacheKey("cover_$mangaId")
    .build()
