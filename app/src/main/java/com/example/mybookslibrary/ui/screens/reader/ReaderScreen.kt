@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.ui.screens.onboarding.ReaderSpotlightOverlay
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch

/**
 * Main reader route that wires ViewModel state/effects to stateless reader UI.
 */
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    onNavigateToChapter: (mangaId: String, chapterId: String, chapterTitle: String) -> Unit = { _, _, _ -> },
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
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState =
        rememberPagerState(
            initialPage = state.lastReadPageIndex,
            // +1 cho trang chuyển tiếp cuối chương (Phase 4 PR-2a); 0 khi chưa có trang.
            pageCount = { if (state.pages.isEmpty()) 0 else state.pages.size + 1 },
        )

    ConfigureReaderSystemBars(
        activity = activity,
        backgroundIsLight = backgroundIsLight,
        overlayIsVisible = state.isOverlayVisible,
        overlayIsLight = readerBarIsLight,
    )

    // Giữ màn hình sáng khi đọc — gắn cờ lên View, gỡ khi rời reader.
    val view = LocalView.current
    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Đăng ký lật trang bằng phím âm lượng khi tính năng bật; huỷ khi tắt hoặc rời màn hình.
    // Holder là singleton process-wide → chỉ gỡ ĐÚNG handler mình đặt (identity check) để
    // tránh race khi 2 màn reader chồng nhau lúc chuyển chương xoá nhầm handler màn mới.
    DisposableEffect(state.volumeKeyNav) {
        val handler: ((Boolean) -> Boolean)? =
            if (state.volumeKeyNav) {
                { volumeUp ->
                    onEvent(if (volumeUp) ReaderEvent.VolumeKeyPrevPage else ReaderEvent.VolumeKeyNextPage)
                    true
                }
            } else {
                null
            }
        ReaderVolumeKeyHandler.onVolumeKey = handler
        onDispose {
            if (ReaderVolumeKeyHandler.onVolumeKey === handler) {
                ReaderVolumeKeyHandler.onVolumeKey = null
            }
        }
    }

    ReaderEffectHandler(
        effects = viewModel.effects,
        listState = listState,
        pagerState = pagerState,
        currentReadingMode = state.currentReadingMode,
        onEvent = onEvent,
        snackbarHostState = snackbarHostState,
        onNavigateToChapter = onNavigateToChapter,
    )

    ReaderProgressEffects(
        state = state,
        listState = listState,
        pagerState = pagerState,
        latestActivePageIndex = latestActivePageIndex,
        hasRestoredInitialPage = hasRestoredInitialPage,
        onEvent = onEvent,
    )

    val prefsDataStore = remember(context) {
        UserPreferencesDataStore(context.userPreferencesDataStore)
    }
    val readerHintDone by prefsDataStore.observeReaderHintDone()
        .collectAsStateWithLifecycle(initialValue = null)
    val hintScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        readerHintDone?.let { hintDone ->
            // The first-install hint owns the full-screen pointer stream. Recreate reader content
            // after it is dismissed so Telephoto and Pager start with fresh gesture detectors.
            key(hintDone) {
                ReaderContentHost(
                    state = state,
                    listState = listState,
                    pagerState = pagerState,
                    readerBarColors = readerBarColors,
                    onBackClick = onBackClick,
                    onEvent = onEvent,
                )
            }
            ReaderSpotlightOverlay(
                visible = !hintDone && state.pages.isNotEmpty(),
                onDismiss = {
                    hintScope.launch { prefsDataStore.setReaderHintDone(true) }
                },
            )
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
