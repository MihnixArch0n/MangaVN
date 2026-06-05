@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModel
import timber.log.Timber

/**
 * Main reader route that wires ViewModel state/effects to stateless reader UI.
 */
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onEvent: (ReaderEvent) -> Unit = viewModel::onEvent
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val backgroundIsLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val readerBarColors = readerBarColors(isLightTheme = backgroundIsLight)
    val readerBarIsLight = readerBarColors.container.compositeOver(Color.Black).luminance() > 0.5f
    val listState = rememberLazyListState()
    val hasRestoredInitialPage = remember { mutableStateOf(false) }
    val latestActivePageIndex = remember { mutableStateOf<Int?>(null) }
    val pagerState =
        rememberPagerState(
            initialPage = state.lastReadPageIndex,
            pageCount = { state.pages.size },
        )

    Timber.d(
        "ReaderScreen composed: chapter=%s mode=%s pages=%d",
        state.chapterTitle,
        state.currentReadingMode,
        state.pages.size,
    )

    ConfigureReaderSystemBars(
        activity = activity,
        backgroundIsLight = backgroundIsLight,
        overlayIsVisible = state.isOverlayVisible,
        overlayIsLight = readerBarIsLight,
    )

    ReaderEffectHandler(
        effects = viewModel.effects,
        pagerState = pagerState,
        currentReadingMode = state.currentReadingMode,
        onEvent = onEvent,
    )

    ReaderProgressEffects(
        state = state,
        listState = listState,
        pagerState = pagerState,
        latestActivePageIndex = latestActivePageIndex,
        hasRestoredInitialPage = hasRestoredInitialPage,
        onEvent = onEvent,
    )

    ReaderContentHost(
        state = state,
        listState = listState,
        pagerState = pagerState,
        readerBarColors = readerBarColors,
        onBackClick = onBackClick,
        onEvent = onEvent,
        modifier = modifier,
    )
}
