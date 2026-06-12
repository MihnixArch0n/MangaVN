package com.example.mybookslibrary.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.ui.screens.components.AppButton
import com.example.mybookslibrary.ui.screens.components.AppButtonStyle
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.CoverShape
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.statusColors
import com.example.mybookslibrary.ui.util.appString

@Composable
internal fun MangaDetailBackdrop(mangaId: String, coverUrl: String?) {
    Box(modifier = Modifier.fillMaxWidth().height(DetailDimensions.BackdropHeight)) {
        AsyncImage(
            model = coverRequest(mangaId, coverUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(radius = DetailDimensions.BlurRadius),
        )
        // Scrim gradient — fade nền background lên cover blur
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = Alphas.EmphasisFaint),
                                    MaterialTheme.colorScheme.background,
                                ),
                            startY = 120f,
                        ),
                    ),
        )
    }
}

@Composable
internal fun MangaDetailHeader(mangaId: String, title: String, coverUrl: String?, tags: List<String>) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .offset(y = DetailDimensions.CoverRowOffset),
        verticalAlignment = Alignment.Bottom,
    ) {
        Card(
            modifier = Modifier.size(DetailDimensions.CoverWidth, DetailDimensions.CoverHeight),
            shape = CoverShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            AsyncImage(
                model = coverRequest(mangaId, coverUrl),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.padding(start = Dimens.SpacingLg + Dimens.SpacingXs, bottom = Dimens.SpacingSm)
                .weight(1f),
        ) {
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    tags.take(2).forEach { tag ->
                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alphas.EmphasisMedium),
                                        MaterialTheme.shapes.extraLarge,
                                    )
                                    .padding(
                                        horizontal = Dimens.SpacingSm + Dimens.SpacingXs,
                                        vertical = Dimens.SpacingXs,
                                    ),
                        ) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingSm))
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
    Column(
        modifier = Modifier
            .padding(horizontal = Dimens.ScreenPaddingCompact)
            .offset(y = DetailDimensions.ActionOffset),
    ) {
        AppButton(
            text = if (firstChapter !=
                null) {
                    appString(R.string.detail_read_now)
                } else {
                    appString(R.string.detail_loading)
                },
            onClick = { firstChapter?.let(onReadNow) },
            enabled = firstChapter != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.SpacingMd))
        AppButton(
            text = if (isInLibrary) {
                appString(R.string.detail_in_library)
            } else {
                appString(R.string.detail_add_to_library)
            },
            onClick = onToggleLibrary,
            style = AppButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun DetailBackButton(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBackClick,
        modifier = modifier.statusBarsPadding().padding(Dimens.SpacingSm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = Alphas.EmphasisHigh)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Lucide.ArrowLeft,
                appString(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun DetailShareButton(onShareClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onShareClick,
        modifier = modifier.statusBarsPadding().padding(Dimens.SpacingSm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = Alphas.EmphasisHigh)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Lucide.Share2,
                contentDescription = appString(R.string.cd_share),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun DetailFavoriteButton(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggleFavorite,
        modifier = modifier.statusBarsPadding().padding(Dimens.SpacingSm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = Alphas.EmphasisHigh)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Lucide.Heart,
                contentDescription =
                    if (isFavorite) {
                        appString(R.string.cd_favorite_remove)
                    } else {
                        appString(R.string.cd_favorite_add)
                    },
                // Cùng màu semantic với StatusChip FAVORITE
                tint =
                    if (isFavorite) {
                        MaterialTheme.statusColors.favorite
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun coverRequest(mangaId: String, coverUrl: String?): ImageRequest =
    ImageRequest
        .Builder(LocalContext.current)
        .data(coverUrl)
        .placeholderMemoryCacheKey("cover_$mangaId")
        .memoryCacheKey("cover_$mangaId")
        .build()
