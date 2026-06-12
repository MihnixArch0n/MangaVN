package com.example.mybookslibrary.ui.screens

import android.text.format.Formatter
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.screens.components.EmptyState
import com.example.mybookslibrary.ui.screens.components.LoadingIndicator
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.Elevations
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.DownloadedChapterUi
import com.example.mybookslibrary.ui.viewmodel.DownloadsEvent
import com.example.mybookslibrary.ui.viewmodel.DownloadsUiState
import com.example.mybookslibrary.ui.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = appString(R.string.downloads_delete_failed)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                DownloadsEvent.DELETE_FAILED -> snackbarHostState.showSnackbar(deleteFailedMessage)
            }
        }
    }

    DownloadsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onDelete = { chapter -> vm.deleteDownload(chapter.mangaId, chapter.chapterId) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsScreenContent(
    uiState: DownloadsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onDelete: (DownloadedChapterUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Confirm trước khi xóa — hành động destructive (file offline mất là tải lại từ đầu)
    var pendingDelete by remember { mutableStateOf<DownloadedChapterUi?>(null) }

    pendingDelete?.let { chapter ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(appString(R.string.downloads_delete)) },
            text = { Text(appString(R.string.downloads_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(chapter)
                        pendingDelete = null
                    },
                ) {
                    Text(
                        appString(R.string.downloads_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(appString(R.string.action_cancel))
                }
            },
        )
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val chapters = uiState.chapters
        when {
            // null = đang scan filesystem — không flash EmptyState
            chapters == null -> LoadingIndicator(modifier = Modifier.fillMaxSize().padding(innerPadding))
            chapters.isEmpty() ->
                EmptyState(
                    title = appString(R.string.downloads_empty_title),
                    subtitle = appString(R.string.downloads_empty_subtitle),
                    icon = Lucide.Download,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            else ->
                DownloadsList(
                    chapters = chapters,
                    totalSizeBytes = uiState.totalSizeBytes,
                    onDeleteRequest = { pendingDelete = it },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
        }
    }
}

@Composable
private fun DownloadsList(
    chapters: List<DownloadedChapterUi>,
    totalSizeBytes: Long,
    onDeleteRequest: (DownloadedChapterUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // List đã sort: nhóm còn title trước, nhóm mất title (mangaTitle null) cuối
    val orphanIndex = chapters.indexOfFirst { it.mangaTitle == null }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = Dimens.ScreenPaddingCompact,
            vertical = Dimens.SpacingLg,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
    ) {
        item(key = "total_size") {
            Text(
                appString(R.string.downloads_total_size, Formatter.formatFileSize(context, totalSizeBytes)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val inLibrary = if (orphanIndex < 0) chapters else chapters.subList(0, orphanIndex)
        val orphans = if (orphanIndex < 0) emptyList() else chapters.subList(orphanIndex, chapters.size)

        items(inLibrary, key = { it.chapterId }) { chapter ->
            DownloadedChapterCard(chapter = chapter, onDelete = { onDeleteRequest(chapter) })
        }
        if (orphans.isNotEmpty()) {
            item(key = "not_in_library_header") {
                Text(
                    appString(R.string.downloads_not_in_library),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.SpacingMd),
                )
            }
            items(orphans, key = { it.chapterId }) { chapter ->
                DownloadedChapterCard(chapter = chapter, onDelete = { onDeleteRequest(chapter) })
            }
        }
    }
}

@Composable
private fun DownloadedChapterCard(
    chapter: DownloadedChapterUi,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevations.Resting),
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
                    chapter.mangaTitle ?: appString(R.string.downloads_not_in_library),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.SpacingXs))
                val sizeLabel = Formatter.formatFileSize(context, chapter.sizeBytes)
                Text(
                    "${chapter.chapterLabel ?: appString(R.string.downloads_unknown_chapter)} · $sizeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Lucide.Trash2,
                    contentDescription = appString(R.string.cd_delete_download),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadsScreenPreview() {
    MyBooksLibraryTheme {
        DownloadsScreenContent(
            uiState = DownloadsUiState(
                chapters = listOf(
                    DownloadedChapterUi(
                        chapterId = "c1",
                        mangaId = "m1",
                        mangaTitle = "One Piece",
                        chapterLabel = "Vol. 1 Ch. 1",
                        sizeBytes = 12_582_912,
                    ),
                    DownloadedChapterUi(
                        chapterId = "c2",
                        mangaId = "m1",
                        mangaTitle = "One Piece",
                        chapterLabel = "Ch. 2",
                        sizeBytes = 9_437_184,
                    ),
                    DownloadedChapterUi(
                        chapterId = "c3",
                        mangaId = "m2",
                        mangaTitle = null,
                        chapterLabel = null,
                        sizeBytes = 5_242_880,
                    ),
                ),
                totalSizeBytes = 27_262_976,
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadsScreenEmptyPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        DownloadsScreenContent(
            uiState = DownloadsUiState(chapters = emptyList()),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onDelete = {},
        )
    }
}
