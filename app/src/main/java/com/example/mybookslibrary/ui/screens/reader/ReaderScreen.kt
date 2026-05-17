package com.example.mybookslibrary.ui.screens.reader

import android.content.Intent
import android.util.Log
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

private const val TAG = "ReaderScreen"

@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- ImageSaver instance (no DI needed — uses bare OkHttpClient internally) ---
    val imageSaver = remember { ImageSaver(context) }

    // --- Page Action Bottom Sheet state ---
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedPageUrl by remember { mutableStateOf<String?>(null) }

    // --- SAF launcher for "Save As" ---
    // Stores the URL temporarily while waiting for SAF result
    var pendingSaveAsUrl by remember { mutableStateOf<String?>(null) }
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/*")
    ) { uri ->
        val url = pendingSaveAsUrl ?: return@rememberLauncherForActivityResult
        pendingSaveAsUrl = null
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch(Dispatchers.IO) {
            try {
                imageSaver.saveToUri(url, uri)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.reader_saved_to_file), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveToUri failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.reader_save_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- Long-press handler ---
    val onPageLongPress: (String) -> Unit = { url ->
        selectedPageUrl = url
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
            .map { (lastVisible, _) -> lastVisible.coerceIn(0, state.pages.lastIndex) }
            .collect(viewModel::onVisiblePageChanged)
    }

    // Restore initial page position for vertical mode (first load only)
    LaunchedEffect(state.pages.size, state.currentReadingMode) {
        if (state.currentReadingMode != ReadingMode.VERTICAL) return@LaunchedEffect
        if (state.pages.isEmpty() || hasRestoredInitialPage.value) return@LaunchedEffect
        listState.scrollToItem(state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex))
        hasRestoredInitialPage.value = true
    }

    // ────────────────────────────────────────────────────────────
    // Mode switch sync: scroll the new mode's state to the current page
    // ────────────────────────────────────────────────────────────
    LaunchedEffect(state.currentReadingMode) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        val targetPage = state.lastReadPageIndex.coerceIn(0, state.pages.lastIndex)
        when (state.currentReadingMode) {
            ReadingMode.VERTICAL -> listState.scrollToItem(targetPage)
            ReadingMode.LTR, ReadingMode.RTL -> pagerState.scrollToPage(targetPage)
        }
    }

    // ────────────────────────────────────────────────────────────
    // Horizontal pager → ViewModel page tracking
    // ────────────────────────────────────────────────────────────
    LaunchedEffect(pagerState, state.currentReadingMode) {
        if (state.currentReadingMode == ReadingMode.VERTICAL) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect(viewModel::onVisiblePageChanged)
    }

    // Listen to tap-zone navigation events and animate pager to target page
    LaunchedEffect(pagerState, state.currentReadingMode) {
        if (state.currentReadingMode == ReadingMode.VERTICAL) return@LaunchedEffect
        viewModel.pageNavigationEvent.collectLatest { targetPage ->
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync progress to Room when leaving
    DisposableEffect(Unit) {
        onDispose { viewModel.syncProgressToRoom() }
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
                    Log.d(TAG, "Tap detected: x=${offset.x}, width=${size.width}, mode=${state.currentReadingMode}, action=$action")

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
    if (showBottomSheet && selectedPageUrl != null) {
        PageActionBottomSheet(
            onDismiss = { showBottomSheet = false },
            onAction = { action ->
                val url = selectedPageUrl ?: return@PageActionBottomSheet
                val pageName = "page_${state.lastReadPageIndex + 1}"

                when (action) {
                    PageAction.QuickSave -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                imageSaver.quickSave(url, pageName)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.reader_saved_to_pictures), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "quickSave failed", e)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.reader_save_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    PageAction.SaveAs -> {
                        pendingSaveAsUrl = url
                        saveAsLauncher.launch("$pageName.jpg")
                    }

                    PageAction.Share -> {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val intent = imageSaver.shareImage(url, pageName)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.reader_sharing), Toast.LENGTH_SHORT).show()
                                    context.startActivity(Intent.createChooser(intent, null))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "shareImage failed", e)
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.reader_share_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
    onPageLongPress: (String) -> Unit,
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
