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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun MangaReviewScreen(onBackClick: () -> Unit) {
    val dummyReviews =
        listOf(
            DummyReview("Tốt 👍", "5 sao\n\nwe ét it để đi he", "30 Jun 2025", "User123"),
            DummyReview(
                "Hay",
                "Cái này giúp cho mình biết được nó có thể giúp gì\n\n💖💞💘",
                "5 Oct 2024",
                "MangaFan99",
            ),
            DummyReview("iPhone xs", "Viettel 4g\n\ntuyệt vời với Siri", "22 Sep 2023", "ArtLover"),
            DummyReview(
                "Great read",
                "I couldn't put this down. The story is engaging and the art is fantastic.",
                "12 Oct 2025",
                "User456",
            ),
            DummyReview(
                "A masterpiece",
                "Truly one of the best mangas I've read in a long time. Highly recommend it to anyone.",
                "05 Nov 2025",
                "FanBoy",
            ),
        )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        appString(R.string.detail_customer_reviews),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Lucide.ArrowLeft, contentDescription = appString(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.ScreenPaddingCompact),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpacingLg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "3.2",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            appString(R.string.review_out_of_5),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(start = Dimens.SpacingXl),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
                    ) {
                        RatingBarRow(stars = 5, progress = 0.6f)
                        RatingBarRow(stars = 4, progress = 0.2f)
                        RatingBarRow(stars = 3, progress = 0.1f)
                        RatingBarRow(stars = 2, progress = 0.05f)
                        RatingBarRow(stars = 1, progress = 0.05f)
                        Text(
                            appString(R.string.review_ratings_count, 177),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End).padding(top = Dimens.SpacingXs),
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.SpacingXl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        appString(R.string.review_section_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            }

            items(dummyReviews) { review ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Dimens.SpacingLg).fillMaxWidth()) {
                        Text(
                            review.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Dimens.SpacingSm))
                        Text(
                            review.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Dimens.SpacingMd))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row {
                                repeat(5) { index ->
                                    Icon(
                                        Lucide.Star,
                                        contentDescription = null,
                                        tint =
                                            if (index < 3) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                                    .copy(alpha = Alphas.EmphasisFaint)
                                            },
                                        modifier = Modifier.size(Dimens.IconXs),
                                    )
                                }
                            }
                            Spacer(Modifier.width(Dimens.SpacingSm))
                            Text(
                                review.date + ", " + review.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun RatingBarRow(stars: Int, progress: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.width(60.dp)) {
            repeat(stars) {
                Icon(
                    Lucide.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(Dimens.SpacingXs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress)
                        .height(Dimens.SpacingXs)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
            )
        }
    }
}
