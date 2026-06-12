package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.ui.screens.components.EmptyState
import com.example.mybookslibrary.ui.screens.components.MangaCoverCard
import com.example.mybookslibrary.ui.screens.components.StatusChip
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.ReadingHistoryViewModel
import java.util.concurrent.TimeUnit

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryScreen(
    onBackClick: () -> Unit,
    onMangaClick: (mangaId: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: ReadingHistoryViewModel = hiltViewModel(),
) {
    val items by vm.historyItems.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.reading_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Lucide.ArrowLeft, contentDescription = appString(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyState(
                title = appString(R.string.reading_history_empty_title),
                subtitle = appString(R.string.reading_history_empty_subtitle),
                icon = Lucide.BookOpen,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(
                    horizontal = Dimens.ScreenPaddingCompact,
                    vertical = Dimens.SpacingLg,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
            ) {
                items(items, key = { it.manga_id }) { item ->
                    ReadingHistoryCard(
                        item = item,
                        onClick = { onMangaClick(item.manga_id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingHistoryCard(
    item: LibraryItemEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.BorderThin),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCoverCard(
                coverUrl = item.cover_url,
                contentDescription = item.title,
                width = Dimens.CoverThumb,
            )
            Spacer(Modifier.width(Dimens.SpacingLg))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Dimens.SpacingXs))
                StatusChip(item.status)
                Spacer(Modifier.height(Dimens.SpacingXs))
                Text(
                    text = appString(R.string.reading_history_last_read, formatRelativeTime(item.updated_at)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(now - timestampMs)
    return when {
        diffDays < 1L -> appString(R.string.reading_history_today)
        diffDays < 2L -> appString(R.string.reading_history_yesterday)
        else -> appString(R.string.reading_history_days_ago, diffDays.toInt())
    }
}
