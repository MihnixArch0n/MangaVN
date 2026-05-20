package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ImportContacts
import com.example.mybookslibrary.domain.model.ReadingMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.R
import androidx.compose.runtime.Composable
import timber.log.Timber

/**
 * Top reader overlay bar anchored inside the parent [BoxScope].
 *
 * Shows the chapter title and a back button only while [isVisible] is true.
 *
 * @param chapterTitle Title rendered in the center area of the bar.
 * @param isVisible Controls the animated visibility of the bar.
 * @param onBackClick Called when the back button is tapped.
 */
@Composable
fun BoxScope.ReaderTopBar(chapterTitle: String, isVisible: Boolean, onBackClick: () -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                Timber.d("ReaderTopBar back clicked")
                onBackClick()
            }) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = appString(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }
    }
}

@Preview(name = "Reader Top Bar", showBackground = true)
@Composable
private fun ReaderTopBarPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ReaderTopBar(
                chapterTitle = "Chapter 12: Lost Pages",
                isVisible = true,
                onBackClick = { }
            )
        }
    }
}

@Preview(name = "Reader Bottom Bar", showBackground = true)
@Composable
private fun ReaderBottomBarPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ReaderBottomBar(
                isVisible = true,
                currentPage = 1,
                totalPages = 18,
                currentReadingMode = ReadingMode.LTR,
                onToggleReadingMode = { }
            )
        }
    }
}

/**
 * Bottom reader overlay bar anchored inside the parent [BoxScope].
 *
 * Displays the current page counter, the pages label, and a reading-mode toggle button.
 * The page counter is rendered as one-based and clamped to the valid range.
 *
 * @param isVisible Controls the animated visibility of the bar.
 * @param currentPage Zero-based current page index used to derive the displayed counter.
 * @param totalPages Total number of pages available in the chapter.
 * @param currentReadingMode Active reading mode used to choose the toggle icon and label.
 * @param onToggleReadingMode Called when the mode toggle button is tapped.
 */
@Composable
fun BoxScope.ReaderBottomBar(
    isVisible: Boolean,
    currentPage: Int,
    totalPages: Int,
    currentReadingMode: ReadingMode,
    onToggleReadingMode: () -> Unit
) {
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val displayPage = (currentPage + 1).coerceIn(1, safeTotalPages)
    val nextReadingModeRes = when (currentReadingMode) {
        ReadingMode.VERTICAL -> R.string.reader_mode_ltr
        ReadingMode.LTR -> R.string.reader_mode_rtl
        ReadingMode.RTL -> R.string.reader_mode_vertical
    }

    // Icon and content description cycle: VERTICAL → LTR → RTL
    val modeIcon = when (currentReadingMode) {
        ReadingMode.VERTICAL -> Icons.AutoMirrored.Filled.MenuBook
        ReadingMode.LTR -> Icons.Filled.ImportContacts
        ReadingMode.RTL -> Icons.Filled.AutoStories
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$displayPage / $safeTotalPages",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = appString(R.string.reader_pages_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            IconButton(onClick = {
                Timber.d("ReaderBottomBar toggle clicked: currentMode=%s", currentReadingMode)
                onToggleReadingMode()
            }) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = appString(
                        R.string.reader_switch_mode_action,
                        appString(nextReadingModeRes)
                    ),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
