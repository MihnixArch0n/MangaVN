@file:Suppress(
    "LongParameterList",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.screens.reader.components.PageActionBottomSheet
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderState

@Composable
internal fun ReaderContentHost(
    state: ReaderState,
    listState: LazyListState,
    pagerState: PagerState,
    onBackClick: () -> Unit,
    onEvent: (ReaderEvent) -> Unit,
    modifier: Modifier = Modifier,
    readerBarColors: ReaderBarColors = readerBarColors(),
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        ReaderContentBody(
            state = state,
            listState = listState,
            pagerState = pagerState,
            onEvent = onEvent,
        )

        ReaderOverlayBars(
            state = state,
            readerBarColors = readerBarColors,
            onBackClick = onBackClick,
            onEvent = onEvent,
        )
    }

    ReaderPageActionSheet(state = state, onEvent = onEvent)
}

@Composable
private fun ReaderContentBody(
    state: ReaderState,
    listState: LazyListState,
    pagerState: PagerState,
    onEvent: (ReaderEvent) -> Unit,
) {
    when {
        state.isLoading -> ReaderCenteredProgress()
        state.error != null -> ReaderCenteredMessage(appString(R.string.error_prefix, state.error))
        state.pages.isEmpty() -> ReaderCenteredMessage(appString(R.string.error_load_pages))
        else -> ReaderPages(state = state, listState = listState, pagerState = pagerState, onEvent = onEvent)
    }
}

@Composable
private fun ReaderPages(
    state: ReaderState,
    listState: LazyListState,
    pagerState: PagerState,
    onEvent: (ReaderEvent) -> Unit,
) {
    when (state.currentReadingMode) {
        ReadingMode.VERTICAL -> {
            VerticalReaderContent(
                pages = state.pages,
                listState = listState,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
        ReadingMode.LTR,
        ReadingMode.RTL,
        -> {
            HorizontalReaderContent(
                pages = state.pages,
                pagerState = pagerState,
                readingMode = state.currentReadingMode,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ReaderCenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.surface)
    }
}

@Composable
private fun ReaderCenteredMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ReaderOverlayBars(
    state: ReaderState,
    readerBarColors: ReaderBarColors,
    onBackClick: () -> Unit,
    onEvent: (ReaderEvent) -> Unit,
) {
    ReaderTopBar(
        chapterTitle = state.chapterTitle,
        isVisible = state.isOverlayVisible,
        colors = readerBarColors,
        onBackClick = onBackClick,
    )
    ReaderBottomBar(
        isVisible = state.isOverlayVisible,
        state =
            ReaderBottomBarState(
                currentPage = state.lastReadPageIndex,
                totalPages = state.pages.size,
                currentReadingMode = state.currentReadingMode,
            ),
        colors = readerBarColors,
        onToggleReadingMode = {
            onEvent(ReaderEvent.CycleReadingMode)
        },
    )
}

@Composable
private fun ReaderPageActionSheet(
    state: ReaderState,
    onEvent: (ReaderEvent) -> Unit,
) {
    if (state.selectedPageActionTarget != null) {
        PageActionBottomSheet(
            onDismiss = {
                onEvent(ReaderEvent.DismissPageActions)
            },
            onAction = { action ->
                onEvent(ReaderEvent.PageActionSelected(action.toReaderPageAction()))
            },
        )
    }
}
