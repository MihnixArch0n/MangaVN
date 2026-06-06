package com.example.mybookslibrary.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.util.appString

@Composable
@Suppress("LongParameterList")
internal fun DiscoverContentList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    spotlight: MangaModel?,
    popularItems: List<MangaModel>,
    newItems: List<MangaModel>,
    exploreItems: List<MangaModel>,
    expandedPopular: Boolean,
    expandedNew: Boolean,
    expandedExplore: Boolean,
    onTogglePopular: () -> Unit,
    onToggleNew: () -> Unit,
    onToggleExplore: () -> Unit,
    onMangaClick: (MangaModel) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        spotlight?.let { manga ->
            item { SectionHeader(appString(R.string.section_spotlight)) }
            item {
                SpotlightCard(
                    manga = manga,
                    onClick = { onMangaClick(manga) },
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        shelfSection(
            titleRes = R.string.section_popular,
            items = popularItems,
            expanded = expandedPopular,
            onToggle = onTogglePopular,
            onMangaClick = onMangaClick,
        )
        shelfSection(
            titleRes = R.string.section_new_releases,
            items = newItems,
            expanded = expandedNew,
            onToggle = onToggleNew,
            onMangaClick = onMangaClick,
        )
        shelfSection(
            titleRes = R.string.section_explore,
            items = exploreItems,
            expanded = expandedExplore,
            onToggle = onToggleExplore,
            onMangaClick = onMangaClick,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.shelfSection(
    @StringRes titleRes: Int,
    items: List<MangaModel>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onMangaClick: (MangaModel) -> Unit,
) {
    if (items.isEmpty()) return
    item { Spacer(Modifier.height(32.dp)) }
    item { SectionHeader(appString(titleRes), expanded, onToggle) }
    item {
        if (expanded) {
            ExpandedBookGrid(items, onMangaClick)
        } else {
            HorizontalBookScroll(items, onMangaClick)
        }
    }
}

@Composable
private fun SectionHeader(title: String, expanded: Boolean = false, onToggle: (() -> Unit)? = null,) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (onToggle != null) {
            Text(
                if (expanded) appString(R.string.action_collapse) else appString(R.string.action_see_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.clickable(onClick = onToggle),
            )
        }
    }
}

@Composable
private fun SpotlightCard(manga: MangaModel, onClick: () -> Unit, modifier: Modifier = Modifier,) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(340.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = manga.coverArt,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                            startY = 300f,
                        ),
                    ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                if (manga.tags.isNotEmpty()) {
                    Box(
                        modifier =
                        Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            manga.tags.first().uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    manga.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HorizontalBookScroll(items: List<MangaModel>, onItemClick: (MangaModel) -> Unit,) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { manga ->
            MangaCardItem(manga, { onItemClick(manga) })
        }
    }
}

@Composable
private fun ExpandedBookGrid(items: List<MangaModel>, onItemClick: (MangaModel) -> Unit,) {
    val chunked = remember(items) { items.chunked(GRID_COLUMNS) }
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        chunked.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { manga ->
                    MangaCardItem(manga, { onItemClick(manga) }, Modifier.weight(1f))
                }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val GRID_COLUMNS = 3

@Composable
private fun MangaCardItem(manga: MangaModel, onClick: () -> Unit, modifier: Modifier = Modifier,) {
    Column(modifier = modifier.width(120.dp).clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            AsyncImage(
                model = manga.coverArt,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            manga.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (manga.tags.isNotEmpty()) {
            Text(
                manga.tags.first(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
