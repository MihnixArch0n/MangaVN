package com.example.mybookslibrary.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.util.appString

@Composable
internal fun PublisherSection(description: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 24.dp).offset(y = DetailDimensions.SynopsisOffset)) {
        Text(
            appString(R.string.detail_from_publisher),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
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
                modifier = Modifier.padding(top = 8.dp).clickable { expanded = true },
            )
        }
    }
}

@Composable
internal fun FirstChapterPreviewSection(pageUrls: List<String>) {
    Spacer(Modifier.height(32.dp).offset(y = DetailDimensions.SynopsisOffset))
    Column(modifier = Modifier.fillMaxWidth().offset(y = DetailDimensions.SynopsisOffset)) {
        Text(
            appString(R.string.detail_from_book),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(pageUrls) { pageUrl ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.width(200.dp).height(300.dp),
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp).clickable(onClick = onReviewClick),
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
private fun ReviewCard(review: DummyReview, modifier: Modifier = Modifier, onClick: () -> Unit,) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(review.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                Text(
                    review.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                repeat(REVIEW_STAR_COUNT) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(review.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                review.username,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ChaptersHeader(expanded: Boolean, modifier: Modifier = Modifier,) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                appString(R.string.detail_chapters),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = appString(R.string.cd_expand_chapters),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun DetailMessage(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

data class DummyReview(val title: String, val body: String, val date: String, val username: String,)

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
