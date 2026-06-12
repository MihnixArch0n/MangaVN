package com.example.mybookslibrary.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.Elevations
import com.example.mybookslibrary.ui.util.appString
import androidx.compose.material3.Icon as M3Icon

@Composable
internal fun PublisherSection(description: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingCompact)
            .offset(y = DetailDimensions.SynopsisOffset),
    ) {
        Text(
            appString(R.string.detail_from_publisher),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Dimens.SpacingMd))
        Box(modifier = Modifier.animateContentSize().clickable { expanded = !expanded }) {
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!expanded) {
            Text(
                appString(R.string.action_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Dimens.SpacingSm).clickable { expanded = true },
            )
        }
    }
}

@Composable
internal fun FirstChapterPreviewSection(pageUrls: List<String>) {
    Spacer(Modifier.height(Dimens.SpacingXxl).offset(y = DetailDimensions.SynopsisOffset))
    Column(modifier = Modifier.fillMaxWidth().offset(y = DetailDimensions.SynopsisOffset)) {
        Text(
            appString(R.string.detail_from_book),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingCompact),
        )
        Spacer(Modifier.height(Dimens.SpacingLg))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingCompact),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(pageUrls) { pageUrl ->
                Card(
                    shape = MaterialTheme.shapes.small,
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevations.Dialog),
                    modifier = Modifier.width(160.dp).aspectRatio(2f / 3f),
                ) {
                    AsyncImage(
                        model = pageUrl,
                        contentDescription = appString(R.string.cd_page_preview),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun CustomerReviewsSection(onReviewClick: () -> Unit) {
    Spacer(Modifier.height(40.dp).offset(y = DetailDimensions.SynopsisOffset))
    Column(modifier = Modifier.fillMaxWidth().offset(y = DetailDimensions.SynopsisOffset)) {
        Text(
            appString(R.string.detail_customer_reviews),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .clickable(onClick = onReviewClick),
        )
        Spacer(Modifier.height(Dimens.SpacingLg))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPaddingCompact),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(dummyReviews) { review ->
                ReviewCard(
                    review = review,
                    modifier = Modifier.fillParentMaxWidth(REVIEW_CARD_WIDTH_FRACTION),
                    onClick = onReviewClick,
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: DummyReview, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingLg).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    review.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    review.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Dimens.SpacingXs))
            Row {
                repeat(REVIEW_STAR_COUNT) {
                    M3Icon(
                        Lucide.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(Dimens.IconXs),
                    )
                }
            }
            Spacer(Modifier.height(Dimens.SpacingSm))
            Text(
                review.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.SpacingSm))
            Text(
                review.username,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ChaptersHeader(expanded: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                appString(R.string.detail_chapters),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            M3Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = appString(R.string.cd_expand_chapters),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Dimens.SpacingMd))
    }
}

@Composable
internal fun DetailMessage(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPaddingCompact),
    )
}

data class DummyReview(val title: String, val body: String, val date: String, val username: String)

private val dummyReviews =
    listOf(
        DummyReview(
            "Great read",
            "I couldn't put this down. The story is engaging and the art is fantastic.",
            "Oct 12, 2025",
            "User123",
        ),
        DummyReview(
            "A masterpiece",
            "Truly one of the best mangas I've read in a long time. Highly recommend it to anyone.",
            "Nov 05, 2025",
            "MangaFan99",
        ),
        DummyReview(
            "Stunning visuals",
            "The attention to detail in every panel is just breathtaking.",
            "Dec 20, 2025",
            "ArtLover",
        ),
    )

private const val REVIEW_CARD_WIDTH_FRACTION = 0.85f
private const val REVIEW_STAR_COUNT = 5
