package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.ui.screens.components.EmptyState
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.DownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DownloadsViewModel = hiltViewModel(),
) {
    val chapters by vm.downloadedChapters.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.downloads_title)) },
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
        if (chapters.isEmpty()) {
            EmptyState(
                title = appString(R.string.downloads_empty_title),
                subtitle = appString(R.string.downloads_empty_subtitle),
                icon = Lucide.Download,
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
                items(chapters, key = { it.chapter_id }) { chapter ->
                    DownloadedChapterCard(
                        chapter = chapter,
                        onDelete = { vm.deleteDownload(chapter.chapter_id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedChapterCard(
    chapter: ChapterProgressEntity,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.BorderThin),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Lucide.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = Dimens.SpacingLg),
            ) {
                Text(
                    appString(R.string.downloads_chapter, chapter.chapter_id),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.SpacingXs))
                val progress = if (chapter.total_pages > 0) {
                    "${chapter.last_read_page}/${chapter.total_pages}"
                } else {
                    "—"
                }
                Text(
                    progress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Lucide.Trash2,
                    contentDescription = appString(R.string.downloads_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
