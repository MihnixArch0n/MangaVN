package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModel
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModel
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModelFactory
import coil3.compose.AsyncImage

@Composable
fun DiscoverScreen() {
    val vm: DiscoverViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            CenteredText("Error: ${uiState.error}")
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(uiState.items, key = { it.id }) { manga ->
                    DiscoverListItem(manga = manga)
                }
            }
        }
    }
}


@Composable
fun SearchScreen() {
    CenteredText("Search")
}

@Composable
fun LibraryScreen(
    onOpenMangaDetail: (mangaId: String) -> Unit
) {
    val context = LocalContext.current

    // Skeleton: tạo database + repository local-first trực tiếp (chưa dùng DI/Hilt).
    val database = androidx.compose.runtime.remember(context) { AppDatabase.getInstance(context) }
    val repository = androidx.compose.runtime.remember(database) {
        LibraryRepository(
            libraryDao = database.libraryDao(),
            chapterDao = database.chapterDao()
        )
    }
    val factory = androidx.compose.runtime.remember(repository) { LibraryViewModelFactory(repository) }

    val vm: LibraryViewModel = viewModel(factory = factory)
    val items by vm.libraryItems.collectAsState(initial = emptyList())
    var pendingRemovalItem by remember { mutableStateOf<LibraryItemEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        items(items, key = { it.manga_id }) { item ->
            ListItem(
                headlineContent = { Text(text = item.title) },
                supportingContent = { Text(text = "Bookmarked") },
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            onOpenMangaDetail(item.manga_id)
                        },
                        onLongClick = {
                            pendingRemovalItem = item
                        }
                    )
            )
        }
    }

    if (pendingRemovalItem != null) {
        BookmarkActionsSheet(
            item = pendingRemovalItem!!,
            onDismiss = { pendingRemovalItem = null },
            onConfirmRemove = { selectedItem ->
                vm.onRemoveBookmark(selectedItem.manga_id)
                pendingRemovalItem = null
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BookmarkActionsSheet(
    item: LibraryItemEntity,
    onDismiss: () -> Unit,
    onConfirmRemove: (LibraryItemEntity) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = item.title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        TextButton(
            onClick = { onConfirmRemove(item) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(text = "Remove bookmark")
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = "Cancel")
        }
    }
}

@Composable
fun MangaDetailScreen(mangaId: String) {
    CenteredText("Manga detail: $mangaId")
}

@Composable
fun SettingScreen() {
    CenteredText("Setting")
}

@Composable
private fun CenteredText(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}


@Composable
private fun DiscoverListItem(manga: MangaModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        AsyncImage(
            model = manga.coverArt,
            contentDescription = manga.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Text(text = manga.title)
    }
}

