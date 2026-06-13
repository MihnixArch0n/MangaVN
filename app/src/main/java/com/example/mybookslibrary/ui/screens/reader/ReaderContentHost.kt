@file:Suppress(
    "LongParameterList",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReaderBackground
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.screens.reader.components.PageActionBottomSheet
import com.example.mybookslibrary.ui.screens.components.LoadingIndicator
import com.example.mybookslibrary.ui.screens.components.LoadingSize
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.ReaderBackgroundBlack
import com.example.mybookslibrary.ui.theme.ReaderBackgroundGray
import com.example.mybookslibrary.ui.theme.ReaderBackgroundWhite
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
                .background(state.background.toColor()),
    ) {
        ReaderContentBody(
            state = state,
            listState = listState,
            pagerState = pagerState,
            onEvent = onEvent,
        )

        // Overlay làm tối theo độ sáng (1.0 = trong suốt). Không đăng ký pointer input → tap xuyên qua.
        if (state.brightness < 1f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 1f - state.brightness)),
            )
        }

        ReaderOverlayBars(
            state = state,
            readerBarColors = readerBarColors,
            onBackClick = onBackClick,
            onEvent = onEvent,
        )
    }

    ReaderPageActionSheet(state = state, onEvent = onEvent)

    if (state.isComfortPanelVisible) {
        ReaderComfortSheet(
            brightness = state.brightness,
            background = state.background,
            onBrightnessChange = { onEvent(ReaderEvent.SetBrightness(it)) },
            onBrightnessChangeFinished = { onEvent(ReaderEvent.CommitBrightness) },
            onBackgroundChange = { onEvent(ReaderEvent.SetBackground(it)) },
            onDismiss = { onEvent(ReaderEvent.ToggleComfortPanel) },
        )
    }
}

// Màu nền reader theo lựa chọn người dùng — token trong ui/theme/Color.kt (không hardcode).
private fun ReaderBackground.toColor(): Color =
    when (this) {
        ReaderBackground.BLACK -> ReaderBackgroundBlack
        ReaderBackground.WHITE -> ReaderBackgroundWhite
        ReaderBackground.GRAY -> ReaderBackgroundGray
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
        state.error != null ->
            ReaderErrorState(
                message = appString(R.string.error_prefix, state.error),
                onRetry = { onEvent(ReaderEvent.RetryLoadPages) },
            )
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
                selectedPageIndex = state.selectedPageActionTarget?.pageIndex,
                nextChapterTitle = state.nextChapterTitle,
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
                nextChapterTitle = state.nextChapterTitle,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ReaderCenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(size = LoadingSize.Large, color = Color.White)
    }
}

@Composable
private fun ReaderCenteredMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimens.SpacingXl),
        )
    }
}

@Composable
private fun ReaderErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = Dimens.SpacingLg),
        ) {
            Text(appString(R.string.action_retry))
        }
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
        isVisible = state.isOverlayVisible || state.error != null,
        colors = readerBarColors,
        onBackClick = onBackClick,
    )
    ReaderBottomBar(
        isVisible = state.isOverlayVisible && state.pages.isNotEmpty(),
        state =
            ReaderBottomBarState(
                currentPage = state.lastReadPageIndex,
                totalPages = state.pages.size,
                currentReadingMode = state.currentReadingMode,
                hasPrevChapter = state.prevChapterId != null,
                hasNextChapter = state.nextChapterId != null,
            ),
        colors = readerBarColors,
        onToggleReadingMode = { onEvent(ReaderEvent.CycleReadingMode) },
        onPageSelected = { pageIndex ->
            onEvent(ReaderEvent.JumpToPage(pageIndex))
        },
        onPrevChapter = { onEvent(ReaderEvent.NavigatePrevChapter) },
        onNextChapter = { onEvent(ReaderEvent.NavigateNextChapter) },
        onComfortClick = { onEvent(ReaderEvent.ToggleComfortPanel) },
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
