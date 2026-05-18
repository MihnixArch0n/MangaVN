package com.example.mybookslibrary.ui.screens.reader

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.model.TapZoneEvaluator
import com.example.mybookslibrary.ui.screens.reader.components.PageAction
import com.example.mybookslibrary.ui.screens.reader.components.PageActionBottomSheet
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModel
import com.example.mybookslibrary.util.storage.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private enum class ReaderToastType {
    SaveFailed,
    ShareFailed
}

private data class SelectedPageActionTarget(
    val pageUrl: String,
    val pageIndex: Int
)

/**
 * Main reader surface that coordinates page rendering, overlay bars, reading-mode sync,
 * and page-level actions such as quick save, save-as, and share.
 *
 * @param onBackClick Invoked when the reader top bar back button is tapped.
 * @param modifier Modifier applied to the root reader container.
 * @param viewModel Reader state owner that provides pages, progress, mode, and navigation events.
 */
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedToFileText = appString(R.string.reader_saved_to_file)
    val savedToPicturesText = appString(R.string.reader_saved_to_pictures)
    val sharingText = appString(R.string.reader_sharing)
    val fallbackError = appString(R.string.error_unexpected)

    // --- ImageSaver instance (no DI needed — uses bare OkHttpClient internally) ---
    val imageSaver = remember { ImageSaver(context) }

    Timber.d("ReaderScreen composed: chapter=%s mode=%s pages=%d", state.chapterTitle, state.currentReadingMode, state.pages.size)

    // --- Page Action Bottom Sheet state ---
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedPageTarget by remember { mutableStateOf<SelectedPageActionTarget?>(null) }
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

    // --- SAF launcher for "Save As" ---
    // Stores the URL temporarily while waiting for SAF result
    var pendingSaveAsTarget by remember { mutableStateOf<SelectedPageActionTarget?>(null) }
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
                launch(Dispatchers.Main) {
                    Toast.makeText(context, savedToFileText, Toast.LENGTH_SHORT).show()
                }
                Timber.d("Reader save-as end: page=%d url=%s uri=%s", displayPage, url, uri)
            } catch (e: Exception) {
                Timber.e(e, "Reader save-as failed: page=%d url=%s uri=%s", displayPage, url, uri)
                withContext(Dispatchers.Main) {
                    errorToastType = ReaderToastType.SaveFailed
                    errorMessageEvent = e.message ?: fallbackError
                }
            }
        }
    }

    // --- Long-press handler ---
    val onPageLongPress: (String, Int) -> Unit = { url, pageIndex ->
        Timber.d("Reader page long-pressed: page=%d url=%s", pageIndex + 1, url)
        selectedPageTarget = SelectedPageActionTarget(pageUrl = url, pageIndex = pageIndex)
        showBottomSheet = true
    }

    // --- Vertical mode state ---
    val listState = rememberLazyListState()
    val hasRestoredInitialPage = remember { mutableStateOf(false) }

    // --- Horizontal mode state ---
    val pagerState = rememberPagerState(
        initialPage = state.lastReadPageIndex,
        pageCount = { state.pages.size }
    )

    // ────────────────────────────────────────────────────────────
    // Vertical scroll → ViewModel page tracking
    // ────────────────────────────────────────────────────────────
    LaunchedEffect(listState, state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            lastVisible to totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (_, totalItemsCount) -> totalItemsCount > 0 }
            .filter { (lastVisible, _) -> lastVisible >= 0 }
            .distinctUntilChanged()
            .map { (lastVisible, totalItemsCount) ->
                val visiblePage = lastVisible.coerceIn(0, state.pages.lastIndex)
                Timber.d("Reader vertical page visible: visible=%d total=%d mode=%s", visiblePage, totalItemsCount, state.currentReadingMode)
                visiblePage
            }
            .collect(viewModel::onVisiblePageChanged)
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
            .collect(viewModel::onVisiblePageChanged)
    }

    // Listen to tap-zone navigation events and animate pager to target page
    LaunchedEffect(pagerState, state.currentReadingMode) {
        if (state.currentReadingMode == ReadingMode.VERTICAL) return@LaunchedEffect
        viewModel.pageNavigationEvent.collectLatest { targetPage ->
            Timber.d("Reader page navigation start: targetPage=%d mode=%s", targetPage, state.currentReadingMode)
            pagerState.animateScrollToPage(targetPage)
            Timber.d("Reader page navigation end: targetPage=%d mode=%s", targetPage, state.currentReadingMode)
        }
    }

    // Sync progress to Room when leaving
    DisposableEffect(Unit) {
        onDispose {
            Timber.d("Reader progress sync start")
            viewModel.syncProgressToRoom()
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
            .pointerInput(state.currentReadingMode) {
                detectTapGestures(onTap = { offset ->
                    val action = TapZoneEvaluator.evaluateTap(
                        x = offset.x,
                        totalWidth = size.width.toFloat(),
                        mode = state.currentReadingMode
                    )
                    Timber.d(
                        "Reader tap detected: x=%.1f width=%d mode=%s action=%s",
                        offset.x,
                        size.width,
                        state.currentReadingMode,
                        action
                    )

                    viewModel.navigateToPage(action)
                })
            }
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
            else -> {
                when (state.currentReadingMode) {
                    ReadingMode.VERTICAL -> {
                        VerticalReaderContent(
                            pages = state.pages,
                            listState = listState,
                            onPageLongPress = onPageLongPress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ReadingMode.LTR, ReadingMode.RTL -> {
                        HorizontalReaderContent(
                            pages = state.pages,
                            pagerState = pagerState,
                            readingMode = state.currentReadingMode,
                            onPageLongPress = onPageLongPress,
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
                val next = when (state.currentReadingMode) {
                    ReadingMode.VERTICAL -> ReadingMode.LTR
                    ReadingMode.LTR -> ReadingMode.RTL
                    ReadingMode.RTL -> ReadingMode.VERTICAL
                }
                viewModel.setReadingMode(next)
            }
        )
    }

    // ────────────────────────────────────────────────────────────
    // Page Action Bottom Sheet
    // ────────────────────────────────────────────────────────────
    if (showBottomSheet && selectedPageTarget != null) {
        PageActionBottomSheet(
            onDismiss = {
                showBottomSheet = false
                selectedPageTarget = null
                Timber.d("Reader page action sheet dismissed")
            },
            onAction = { action ->
                val target = selectedPageTarget ?: return@PageActionBottomSheet
                val pageNumber = target.pageIndex + 1
                val chapterSlug = state.chapterTitle
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_')
                    .ifBlank { "chapter" }
                val saveAsExtension = target.pageUrl
                    .toUri()
                    .lastPathSegment
                    ?.substringAfterLast('.', "jpg")
                    ?.takeIf { it.isNotBlank() }
                    ?: "jpg"
                val pageName = "${chapterSlug}_p${pageNumber}_${target.pageUrl.hashCode().toUInt().toString(16)}"

                when (action) {
                    PageAction.QuickSave -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                Timber.d("Reader quick-save start: page=%d url=%s", pageNumber, target.pageUrl)
                                imageSaver.quickSave(target.pageUrl, pageName)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, savedToPicturesText, Toast.LENGTH_SHORT).show()
                                }
                                Timber.d("Reader quick-save end: page=%d url=%s", pageNumber, target.pageUrl)
                            } catch (e: Exception) {
                                Timber.e(e, "Reader quick-save failed: page=%d url=%s", pageNumber, target.pageUrl)
                                withContext(Dispatchers.Main) {
                                    errorToastType = ReaderToastType.SaveFailed
                                    errorMessageEvent = e.message ?: fallbackError
                                }
                            }
                        }
                    }

                    PageAction.SaveAs -> {
                        Timber.d("Reader save-as selected: page=%d url=%s", pageNumber, target.pageUrl)
                        pendingSaveAsTarget = target
                        saveAsLauncher.launch("$pageName.$saveAsExtension")
                    }

                    PageAction.Share -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                Timber.d("Reader share start: page=%d url=%s", pageNumber, target.pageUrl)
                                val intent = imageSaver.shareImage(target.pageUrl, pageName)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, sharingText, Toast.LENGTH_SHORT).show()
                                    context.startActivity(Intent.createChooser(intent, null))
                                }
                                Timber.d("Reader share end: page=%d url=%s", pageNumber, target.pageUrl)
                            } catch (e: Exception) {
                                Timber.e(e, "Reader share failed: page=%d url=%s", pageNumber, target.pageUrl)
                                withContext(Dispatchers.Main) {
                                    errorToastType = ReaderToastType.ShareFailed
                                    errorMessageEvent = e.message ?: fallbackError
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun VerticalReaderContent(
    pages: List<String>,
    listState: LazyListState,
    onPageLongPress: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, state = listState) {
        itemsIndexed(items = pages, key = { _, page -> page }) { index, page ->
            MangaPageItem(
                imageUrl = page,
                index = index,
                onLongPress = onPageLongPress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Reader content and bar components moved to MangaPageItem.kt and ReaderBars.kt for modularity.
