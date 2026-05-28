package com.example.mybookslibrary.ui.screens.reader

import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.screens.reader.components.PageAction
import com.example.mybookslibrary.ui.screens.reader.components.PageActionBottomSheet
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.util.findActivePageIndex
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderPageAction
import com.example.mybookslibrary.ui.viewmodel.ReaderPageActionTarget
import com.example.mybookslibrary.ui.viewmodel.ReaderUiEffect
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModel
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.util.storage.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import timber.log.Timber

private enum class ReaderToastType {
    SaveFailed,
    ShareFailed
}

/**
 * Main reader surface that coordinates page rendering, overlay bars, reading-mode sync,
 * progress tracking, and page-level actions such as quick save, save-as, and share.
 *
 * Logging notes:
 * - vertical page changes are logged after the active-item calculation,
 * - flushes on dispose are logged with the final page index,
 * - mode sync and page navigation events keep their existing debug traces.
 */
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
@Composable
@OptIn(FlowPreview::class)
fun ReaderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onEvent: (ReaderEvent) -> Unit = viewModel::onEvent
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val backgroundIsLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val savedToFileText = appString(R.string.reader_saved_to_file)
    val savedToPicturesText = appString(R.string.reader_saved_to_pictures)
    val sharingText = appString(R.string.reader_sharing)
    val fallbackError = appString(R.string.error_unexpected)

    // --- ImageSaver instance (no DI needed — uses bare OkHttpClient internally) ---
    val imageSaver = remember { ImageSaver(context) }

    Timber.d("ReaderScreen composed: chapter=%s mode=%s pages=%d", state.chapterTitle, state.currentReadingMode, state.pages.size)

    // --- Page Action result toast state ---
    var errorMessageEvent by remember { mutableStateOf<String?>(null) }
    var errorToastType by remember { mutableStateOf(ReaderToastType.SaveFailed) }

    val saveFailedText = appString(R.string.reader_save_failed, errorMessageEvent ?: fallbackError)
    val shareFailedText = appString(R.string.reader_share_failed, errorMessageEvent ?: fallbackError)

    LaunchedEffect(errorMessageEvent, errorToastType) {
        if (errorMessageEvent != null) {
            val toastText = when (errorToastType) {
                ReaderToastType.SaveFailed -> saveFailedText
                ReaderToastType.ShareFailed -> shareFailedText
            }
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            errorMessageEvent = null
        }
    }

    DisposableEffect(activity, backgroundIsLight) {
        val lightStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        val darkStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)

        activity?.enableEdgeToEdge(
            statusBarStyle = darkStyle,
            navigationBarStyle = darkStyle
        )

        onDispose {
            activity?.enableEdgeToEdge(
                statusBarStyle = if (backgroundIsLight) lightStyle else darkStyle,
                navigationBarStyle = if (backgroundIsLight) lightStyle else darkStyle
            )
        }
    }

    // --- SAF launcher for "Save As" ---
    // Stores the URL temporarily while waiting for SAF result
    var pendingSaveAsTarget by remember { mutableStateOf<ReaderPageActionTarget?>(null) }
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/*")
    ) { uri ->
        val target = pendingSaveAsTarget ?: return@rememberLauncherForActivityResult
        pendingSaveAsTarget = null
        val url = target.pageUrl
        val displayPage = target.pageIndex + 1
        if (uri == null) {
            Timber.d("Reader save-as cancelled: page=%d url=%s", displayPage, url)
            return@rememberLauncherForActivityResult
        }

        Timber.d("Reader save-as requested: page=%d url=%s uri=%s", displayPage, url, uri)

        scope.launch(Dispatchers.IO) {
            try {
                Timber.d("Reader save-as start: page=%d url=%s uri=%s", displayPage, url, uri)
                imageSaver.saveToUri(url, uri)
                withContext(Dispatchers.Main) {
                    onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.SaveAs))
                }
                Timber.d("Reader save-as end: page=%d url=%s uri=%s", displayPage, url, uri)
            } catch (e: Exception) {
                Timber.e(e, "Reader save-as failed: page=%d url=%s uri=%s", displayPage, url, uri)
                withContext(Dispatchers.Main) {
                    onEvent(
                        ReaderEvent.PageActionFailed(
                            action = ReaderPageAction.SaveAs,
                            message = e.message ?: fallbackError
                        )
                    )
                }
            }
        }
    }

    // --- Vertical mode state ---
    // Keep the latest active page so we can flush it immediately on dispose.
    val listState = rememberLazyListState()
    val hasRestoredInitialPage = remember { mutableStateOf(false) }
    val latestActivePageIndex = remember { mutableStateOf<Int?>(null) }

    // --- Horizontal mode state ---
    // startPageIndex arrives through navigation and SavedStateHandle as
    // ReaderState.lastReadPageIndex, so the pager resumes on the saved page.
    val pagerState = rememberPagerState(
        initialPage = state.lastReadPageIndex,
        pageCount = { state.pages.size }
    )

    LaunchedEffect(viewModel, pagerState, state.currentReadingMode) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReaderUiEffect.NavigateToPage -> {
                    if (state.currentReadingMode != ReadingMode.VERTICAL) {
                        Timber.d(
                            "Reader page navigation start: targetPage=%d mode=%s",
                            effect.pageIndex,
                            state.currentReadingMode
                        )
                        pagerState.animateScrollToPage(effect.pageIndex)
                        Timber.d("Reader page navigation end: targetPage=%d", effect.pageIndex)
                    }
                }
                is ReaderUiEffect.QuickSavePage -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val pageNumber = effect.target.pageIndex + 1
                            Timber.d("Reader quick-save start: page=%d url=%s", pageNumber, effect.target.pageUrl)
                            imageSaver.quickSave(effect.target.pageUrl, effect.fileName)
                            withContext(Dispatchers.Main) {
                                onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.QuickSave))
                            }
                            Timber.d("Reader quick-save end: page=%d url=%s", pageNumber, effect.target.pageUrl)
                        } catch (e: Exception) {
                            Timber.e(e, "Reader quick-save failed: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                            withContext(Dispatchers.Main) {
                                onEvent(
                                    ReaderEvent.PageActionFailed(
                                        action = ReaderPageAction.QuickSave,
                                        message = e.message ?: fallbackError
                                    )
                                )
                            }
                        }
                    }
                }
                is ReaderUiEffect.SavePageAs -> {
                    Timber.d(
                        "Reader save-as selected: page=%d url=%s",
                        effect.target.pageIndex + 1,
                        effect.target.pageUrl
                    )
                    pendingSaveAsTarget = effect.target
                    saveAsLauncher.launch("${effect.fileName}.${effect.extension}")
                }
                is ReaderUiEffect.SharePage -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val pageNumber = effect.target.pageIndex + 1
                            Timber.d("Reader share start: page=%d url=%s", pageNumber, effect.target.pageUrl)
                            val intent = imageSaver.shareImage(effect.target.pageUrl, effect.fileName)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, sharingText, Toast.LENGTH_SHORT).show()
                                context.startActivity(Intent.createChooser(intent, null))
                                onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.Share))
                            }
                            Timber.d("Reader share end: page=%d url=%s", pageNumber, effect.target.pageUrl)
                        } catch (e: Exception) {
                            Timber.e(e, "Reader share failed: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                            withContext(Dispatchers.Main) {
                                onEvent(
                                    ReaderEvent.PageActionFailed(
                                        action = ReaderPageAction.Share,
                                        message = e.message ?: fallbackError
                                    )
                                )
                            }
                        }
                    }
                }
                is ReaderUiEffect.ShowPageActionResult -> {
                    if (effect.errorMessage == null) {
                        val text = when (effect.action) {
                            ReaderPageAction.QuickSave -> savedToPicturesText
                            ReaderPageAction.SaveAs -> savedToFileText
                            ReaderPageAction.Share -> sharingText
                        }
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    } else {
                        errorToastType = when (effect.action) {
                            ReaderPageAction.Share -> ReaderToastType.ShareFailed
                            ReaderPageAction.QuickSave,
                            ReaderPageAction.SaveAs -> ReaderToastType.SaveFailed
                        }
                        errorMessageEvent = effect.errorMessage
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Vertical scroll → ViewModel page tracking
    // ────────────────────────────────────────────────────────────
    LaunchedEffect(listState, state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.findActivePageIndex() }
            .filter { it >= 0 }
            .onEach { index ->
                latestActivePageIndex.value = index
                Timber.d("Reader vertical active-page candidate: page=%d", index)
            }
            .distinctUntilChanged()
            .debounce(300)
            .map { index ->
                val visiblePage = index.coerceIn(0, state.pages.lastIndex)
                Timber.d("Reader vertical page active: page=%d mode=%s", visiblePage, state.currentReadingMode)
                visiblePage
            }
            .collect { index ->
                onEvent(ReaderEvent.VisiblePageChanged(index))
            }
    }

    // Restore initial page position for vertical mode (first load only)
    LaunchedEffect(state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty() || hasRestoredInitialPage.value) return@LaunchedEffect
        Timber.d("Reader vertical restore start: targetPage=%d", state.lastReadPageIndex)
        listState.scrollToItem(state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex))
        hasRestoredInitialPage.value = true
        Timber.d("Reader vertical restore end: targetPage=%d", state.lastReadPageIndex)
    }

    // ────────────────────────────────────────────────────────────
    // Mode switch sync: scroll the new mode's state to the current page
    // ────────────────────────────────────────────────────────────
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

    // ────────────────────────────────────────────────────────────
    // Horizontal pager → ViewModel page tracking
    // ────────────────────────────────────────────────────────────
    LaunchedEffect(pagerState, state.currentReadingMode) {
        if (state.currentReadingMode == ReadingMode.VERTICAL) return@LaunchedEffect
        Timber.d("Reader horizontal page tracking active: mode=%s", state.currentReadingMode)
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .map { page ->
                Timber.d("Reader horizontal page visible: page=%d mode=%s", page, state.currentReadingMode)
                page
            }
            .collect { page ->
                onEvent(ReaderEvent.VisiblePageChanged(page))
            }
    }

    // Sync progress to Room when leaving
    DisposableEffect(Unit) {
        onDispose {
            Timber.d("Reader progress sync start: finalPage=%s", latestActivePageIndex.value?.toString() ?: "<none>")
            onEvent(ReaderEvent.FlushProgress(latestActivePageIndex.value))
            Timber.d("Reader progress sync end")
        }
    }

    // ────────────────────────────────────────────────────────────
    // Layout: Gesture Layer → Content → Overlay Bars
    // ────────────────────────────────────────────────────────────
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.surface)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = appString(R.string.error_prefix, state.error ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
            state.pages.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = appString(R.string.error_load_pages),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
            else -> {
                when (state.currentReadingMode) {
                    ReadingMode.VERTICAL -> {
                        VerticalReaderContent(
                            pages = state.pages,
                            listState = listState,
                            onEvent = onEvent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ReadingMode.LTR, ReadingMode.RTL -> {
                        HorizontalReaderContent(
                            pages = state.pages,
                            pagerState = pagerState,
                            readingMode = state.currentReadingMode,
                            onEvent = onEvent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        ReaderTopBar(state.chapterTitle, state.isOverlayVisible, onBackClick)
        ReaderBottomBar(
            isVisible = state.isOverlayVisible,
            currentPage = state.lastReadPageIndex,
            totalPages = state.pages.size,
            currentReadingMode = state.currentReadingMode,
            onToggleReadingMode = {
                onEvent(ReaderEvent.CycleReadingMode)
            }
        )
    }

    // ────────────────────────────────────────────────────────────
    // Page Action Bottom Sheet
    // ────────────────────────────────────────────────────────────
    if (state.selectedPageActionTarget != null) {
        PageActionBottomSheet(
            onDismiss = {
                onEvent(ReaderEvent.DismissPageActions)
                Timber.d("Reader page action sheet dismissed")
            },
            onAction = { action ->
                onEvent(ReaderEvent.PageActionSelected(action.toReaderPageAction()))
            }
        )
    }
}

@Composable
private fun VerticalReaderContent(
    pages: List<String>,
    listState: LazyListState,
    onEvent: (ReaderEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 3f))
    var containerWidthPx by remember { mutableStateOf(0) }
    var containerHeightPx by remember { mutableStateOf(0) }

    LaunchedEffect(zoomableState) {
        snapshotFlow { zoomableState.zoomFraction }
            .distinctUntilChanged()
            .collect { zoomFraction ->
                Timber.d("Reader webtoon global zoom changed: zoomFraction=%s", zoomFraction)
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                containerWidthPx = it.width
                containerHeightPx = it.height
            }
            .zoomable(
                state = zoomableState,
                onClick = { offset ->
                    Timber.d("Reader webtoon container tap: x=%.1f y=%.1f", offset.x, offset.y)
                    onEvent(
                        ReaderEvent.TapOnScreen(
                            x = offset.x,
                            y = offset.y,
                            width = containerWidthPx.toFloat(),
                            height = containerHeightPx.toFloat()
                        )
                    )
                },
                onLongClick = { offset ->
                    val pageIndex = listState.findPageIndexAtViewportOffset(offset.y)
                    val pageUrl = pages.getOrNull(pageIndex)
                    Timber.d(
                        "Reader webtoon container long-click: x=%.1f y=%.1f page=%d url=%s",
                        offset.x,
                        offset.y,
                        pageIndex + 1,
                        pageUrl
                    )
                    if (pageUrl != null && pageIndex >= 0) {
                        onEvent(ReaderEvent.PageLongPressed(pageUrl, pageIndex))
                    }
                }
            )
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            itemsIndexed(items = pages, key = { _, page -> page }) { index, page ->
                WebtoonPageItem(
                    imageUrl = page,
                    index = index,
                    onTap = { x, y, width, height ->
                        onEvent(ReaderEvent.TapOnScreen(x, y, width, height))
                    },
                    onLongPress = { url, pageIndex ->
                        onEvent(ReaderEvent.PageLongPressed(url, pageIndex))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun LazyListState.findPageIndexAtViewportOffset(y: Float): Int {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { visibleItem ->
        y >= visibleItem.offset && y <= visibleItem.offset + visibleItem.size
    }
    return item?.index ?: layoutInfo.findActivePageIndex()
}

// Reader content and bar components moved to MangaPageItem.kt and ReaderBars.kt for modularity.

private val PreviewReaderPages = listOf(
    "https://example.com/reader/page-1.jpg",
    "https://example.com/reader/page-2.jpg",
    "https://example.com/reader/page-3.jpg"
)

@Preview(name = "Reader - Horizontal", showBackground = true)
@Composable
private fun ReaderHorizontalPreview() {
    MyBooksLibraryTheme {
        ReaderPreviewLayout(
            chapterTitle = "Chapter 12: Lost Pages",
            pages = PreviewReaderPages,
            currentPage = 1,
            readingMode = ReadingMode.LTR
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "Reader - Vertical", showBackground = true)
@Composable
private fun ReaderVerticalPreview() {
    MyBooksLibraryTheme {
        ReaderPreviewLayout(
            chapterTitle = "Chapter 12: Lost Pages",
            pages = PreviewReaderPages,
            currentPage = 1,
            readingMode = ReadingMode.VERTICAL
        )
    }
}

@Composable
private fun ReaderPreviewLayout(
    chapterTitle: String,
    pages: List<String>,
    currentPage: Int,
    readingMode: ReadingMode
) {
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0)),
        pageCount = { pages.size }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (readingMode) {
            ReadingMode.VERTICAL -> {
                VerticalReaderContent(
                    pages = pages,
                    listState = listState,
                    onEvent = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
            ReadingMode.LTR, ReadingMode.RTL -> {
                HorizontalReaderContent(
                    pages = pages,
                    pagerState = pagerState,
                    readingMode = readingMode,
                    onEvent = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        ReaderTopBar(
            chapterTitle = chapterTitle,
            isVisible = true,
            onBackClick = { }
        )
        ReaderBottomBar(
            isVisible = true,
            currentPage = currentPage,
            totalPages = pages.size,
            currentReadingMode = readingMode,
            onToggleReadingMode = { }
        )
    }
}

private fun PageAction.toReaderPageAction(): ReaderPageAction = when (this) {
    PageAction.QuickSave -> ReaderPageAction.QuickSave
    PageAction.SaveAs -> ReaderPageAction.SaveAs
    PageAction.Share -> ReaderPageAction.Share
}
