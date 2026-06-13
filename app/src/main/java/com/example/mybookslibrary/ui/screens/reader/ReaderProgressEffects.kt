@file:Suppress(
    "LongMethod",
    "LongParameterList",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.util.findActivePageIndex
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@OptIn(FlowPreview::class)
@Composable
internal fun ReaderProgressEffects(
    state: ReaderState,
    listState: LazyListState,
    pagerState: PagerState,
    latestActivePageIndex: MutableState<Int?>,
    hasRestoredInitialPage: MutableState<Boolean>,
    onEvent: (ReaderEvent) -> Unit,
) {
    LaunchedEffect(listState, state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty()) return@LaunchedEffect
        var wasOnTransition = false
        snapshotFlow { listState.layoutInfo.findActivePageIndex() }
            .filter { it >= 0 }
            .onEach { index ->
                latestActivePageIndex.value = index.coerceAtMost(state.pages.lastIndex)
                Timber.v("Reader vertical active-page candidate: page=%d", index)
            }.distinctUntilChanged()
            .debounce(300)
            .collect { index ->
                // Item cuối (index == pages.size) là trang chuyển tiếp chương.
                if (index >= state.pages.size) {
                    onEvent(ReaderEvent.ReachedTransitionPage)
                    wasOnTransition = true
                } else {
                    if (wasOnTransition) onEvent(ReaderEvent.LeftTransitionPage)
                    wasOnTransition = false
                    val visiblePage = index.coerceIn(0, state.pages.lastIndex)
                    Timber.v("Reader vertical page active: page=%d mode=%s", visiblePage, state.currentReadingMode)
                    onEvent(ReaderEvent.VisiblePageChanged(visiblePage))
                }
            }
    }

    LaunchedEffect(state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty() || hasRestoredInitialPage.value) return@LaunchedEffect
        Timber.v("Reader vertical restore start: targetPage=%d", state.lastReadPageIndex)
        listState.scrollToItem(state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex))
        hasRestoredInitialPage.value = true
        Timber.v("Reader vertical restore end: targetPage=%d", state.lastReadPageIndex)
    }

    LaunchedEffect(state.currentReadingMode) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        val targetPage = state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex)
        Timber.v("Reader mode sync start: mode=%s targetPage=%d", state.currentReadingMode, targetPage)
        when (state.currentReadingMode) {
            ReadingMode.VERTICAL -> listState.scrollToItem(targetPage)
            ReadingMode.LTR, ReadingMode.RTL -> pagerState.scrollToPage(targetPage)
        }
        Timber.v("Reader mode sync end: mode=%s targetPage=%d", state.currentReadingMode, targetPage)
    }

    LaunchedEffect(pagerState, state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode == ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty()) return@LaunchedEffect
        Timber.v("Reader horizontal page tracking active: mode=%s", state.currentReadingMode)
        var wasOnTransition = false
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                // Trang ảo cuối (index == pages.size) là trang chuyển tiếp chương.
                if (page >= state.pages.size) {
                    onEvent(ReaderEvent.ReachedTransitionPage)
                    wasOnTransition = true
                } else {
                    if (wasOnTransition) onEvent(ReaderEvent.LeftTransitionPage)
                    wasOnTransition = false
                    val visiblePage = page.coerceIn(0, state.pages.lastIndex)
                    latestActivePageIndex.value = visiblePage
                    Timber.v("Reader horizontal page settled: page=%d mode=%s", visiblePage, state.currentReadingMode)
                    onEvent(ReaderEvent.VisiblePageChanged(visiblePage))
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            Timber.v("Reader progress sync start: finalPage=%s", latestActivePageIndex.value?.toString() ?: "<none>")
            onEvent(ReaderEvent.FlushProgress(latestActivePageIndex.value))
            Timber.v("Reader progress sync end")
        }
    }
}
