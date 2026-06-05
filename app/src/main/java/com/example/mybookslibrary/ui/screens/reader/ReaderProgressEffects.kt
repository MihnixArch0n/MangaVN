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
import kotlinx.coroutines.flow.map
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
        snapshotFlow { listState.layoutInfo.findActivePageIndex() }
            .filter { it >= 0 }
            .onEach { index ->
                latestActivePageIndex.value = index
                Timber.d("Reader vertical active-page candidate: page=%d", index)
            }.distinctUntilChanged()
            .debounce(300)
            .map { index ->
                val visiblePage = index.coerceIn(0, state.pages.lastIndex)
                Timber.d("Reader vertical page active: page=%d mode=%s", visiblePage, state.currentReadingMode)
                visiblePage
            }.collect { index ->
                onEvent(ReaderEvent.VisiblePageChanged(index))
            }
    }

    LaunchedEffect(state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty() || hasRestoredInitialPage.value) return@LaunchedEffect
        Timber.d("Reader vertical restore start: targetPage=%d", state.lastReadPageIndex)
        listState.scrollToItem(state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex))
        hasRestoredInitialPage.value = true
        Timber.d("Reader vertical restore end: targetPage=%d", state.lastReadPageIndex)
    }

    LaunchedEffect(state.currentReadingMode) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        val targetPage = state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex)
        Timber.d("Reader mode sync start: mode=%s targetPage=%d", state.currentReadingMode, targetPage)
        when (state.currentReadingMode) {
            ReadingMode.VERTICAL -> listState.scrollToItem(targetPage)
            ReadingMode.LTR, ReadingMode.RTL -> pagerState.scrollToPage(targetPage)
        }
        Timber.d("Reader mode sync end: mode=%s targetPage=%d", state.currentReadingMode, targetPage)
    }

    LaunchedEffect(pagerState, state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode == ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty()) return@LaunchedEffect
        Timber.d("Reader horizontal page tracking active: mode=%s", state.currentReadingMode)
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .map { page ->
                val visiblePage = page.coerceIn(0, state.pages.lastIndex)
                latestActivePageIndex.value = visiblePage
                Timber.d("Reader horizontal page settled: page=%d mode=%s", visiblePage, state.currentReadingMode)
                visiblePage
            }.collect { page ->
                onEvent(ReaderEvent.VisiblePageChanged(page))
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            Timber.d("Reader progress sync start: finalPage=%s", latestActivePageIndex.value?.toString() ?: "<none>")
            onEvent(ReaderEvent.FlushProgress(latestActivePageIndex.value))
            Timber.d("Reader progress sync end")
        }
    }
}
