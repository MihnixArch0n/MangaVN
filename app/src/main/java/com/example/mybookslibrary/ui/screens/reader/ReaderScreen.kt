package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModel
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModelFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun ReaderScreen(
    chapterTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = ReaderViewModelFactory(chapterTitle)
    val viewModel: ReaderViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.pages.size) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it.coerceIn(0, state.pages.lastIndex) }
            .distinctUntilChanged()
            .filter { it >= 0 }
            .collect(viewModel::onVisiblePageChanged)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.syncProgressToRoom()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.toggleOverlay() })
            }
    ) {
        VerticalReaderContent(
            pages = state.pages,
            listState = listState,
            modifier = Modifier.fillMaxSize()
        )

        ReaderTopBar(
            chapterTitle = state.chapterTitle,
            isVisible = state.isOverlayVisible,
            onBackClick = onBackClick
        )

        ReaderBottomBar(
            isVisible = state.isOverlayVisible,
            currentPage = state.lastReadPageIndex,
            totalPages = state.pages.size
        )
    }
}

@Composable
private fun VerticalReaderContent(
    pages: List<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        itemsIndexed(
            items = pages,
            key = { index, _ -> index }
        ) { index, page ->
            AsyncImage(
                model = page,
                contentDescription = "Reader page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.ReaderTopBar(
    chapterTitle: String,
    isVisible: Boolean,
    onBackClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
            TopAppBar(
                title = {
                    Text(
                        text = chapterTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun BoxScope.ReaderBottomBar(
    isVisible: Boolean,
    currentPage: Int,
    totalPages: Int
) {
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val displayCurrentPage = (currentPage + 1).coerceIn(1, safeTotalPages)
    val progressText = "$displayCurrentPage / $safeTotalPages"

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Reader settings",
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}
