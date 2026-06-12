package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleStop
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ChapterDownloadState
import com.example.mybookslibrary.domain.model.ChapterDownloadStatus
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.ui.screens.components.StyledDropdownMenu
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString

@Composable
fun VolumeHeader(volume: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Dimens.ScreenPaddingCompact, vertical = Dimens.SpacingSm + 2.dp),
    ) {
        Text(
            text = volume,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun ChapterRow(
    chapter: ChapterWithProgressModel,
    chapterTitle: String,
    onClick: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkUnread: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isCompleted = chapter.status == ChapterReadingStatus.COMPLETED
    val contentAlpha = if (isCompleted) 0.55f else 1f

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { menuExpanded = true },
                    ).padding(horizontal = Dimens.ScreenPaddingCompact, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapterTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!chapter.title.isNullOrBlank()) {
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (chapter.status) {
                    ChapterReadingStatus.UNREAD ->
                        Text(
                            appString(R.string.status_unread),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        )
                    ChapterReadingStatus.READING ->
                        Text(
                            text = "${appString(
                                R.string.status_reading,
                            )} · Page ${chapter.lastReadPage + 1}/${chapter.totalPages.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    ChapterReadingStatus.COMPLETED ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Lucide.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.SpacingLg),
                            )
                            Spacer(Modifier.width(Dimens.SpacingXs))
                            Text(
                                appString(R.string.status_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }
            Text(
                appString(R.string.detail_pages_suffix, chapter.totalPages),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
            Spacer(Modifier.width(Dimens.SpacingSm))
            ChapterDownloadIndicator(
                state = chapter.downloadState,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                onDeleteDownload = onDeleteDownload,
            )
        }

        StyledDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            if (chapter.status != ChapterReadingStatus.COMPLETED) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.chapter_mark_completed)) },
                    onClick = {
                        menuExpanded = false
                        onMarkCompleted()
                    },
                )
            }
            if (chapter.status != ChapterReadingStatus.UNREAD) {
                DropdownMenuItem(
                    text = { Text(appString(R.string.chapter_mark_unread)) },
                    onClick = {
                        menuExpanded = false
                        onMarkUnread()
                    },
                )
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimens.BorderThin)
                .padding(horizontal = Dimens.ScreenPaddingCompact)
                .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun ChapterDownloadIndicator(
    state: ChapterDownloadState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    when (state.status) {
        ChapterDownloadStatus.NOT_DOWNLOADED -> {
            IconButton(onClick = onStartDownload) {
                Icon(
                    imageVector = Lucide.Download,
                    contentDescription = appString(R.string.chapter_download),
                )
            }
        }
        ChapterDownloadStatus.PENDING -> {
            IconButton(onClick = onCancelDownload) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.IconMd),
                    strokeWidth = Dimens.StrokeLight,
                    color = tint,
                )
            }
        }
        ChapterDownloadStatus.DOWNLOADING -> {
            IconButton(onClick = onCancelDownload) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { state.progressPercent.coerceIn(0, 100) / 100f },
                        modifier = Modifier.size(Dimens.IconDefault),
                        strokeWidth = Dimens.StrokeLight,
                        color = tint,
                    )
                    Icon(
                        imageVector = Lucide.CircleStop,
                        contentDescription = appString(R.string.chapter_cancel_download),
                        modifier = Modifier.size(Dimens.IconXs),
                        tint = tint,
                    )
                }
            }
        }
        ChapterDownloadStatus.DOWNLOADED -> {
            IconButton(onClick = onDeleteDownload) {
                Icon(
                    imageVector = Lucide.CircleCheck,
                    contentDescription = appString(R.string.chapter_delete_download),
                    tint = tint,
                )
            }
        }
        ChapterDownloadStatus.ERROR -> {
            IconButton(onClick = onStartDownload) {
                Icon(
                    imageVector = Lucide.CircleAlert,
                    contentDescription = state.errorMessage ?: appString(R.string.chapter_download_error),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun buildChapterTitle(ch: ChapterWithProgressModel): String {
    val vol = if (!ch.volume.isNullOrBlank()) appString(R.string.chapter_vol_prefix, ch.volume) else ""
    val num =
        if (!ch.chapterNumber.isNullOrBlank()) {
            appString(
                R.string.chapter_num_prefix,
                ch.chapterNumber,
            )
        } else {
            appString(R.string.chapter_oneshot)
        }
    return "$vol$num"
}
