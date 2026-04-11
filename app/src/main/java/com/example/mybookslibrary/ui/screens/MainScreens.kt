package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.remote.NetworkModule
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModel
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModelFactory
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModel
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModelFactory
import coil.compose.AsyncImage

@Composable
fun DiscoverScreen() {
    val repository = remember { MangaRepository(NetworkModule.mangaDexApi) }
    val factory = remember(repository) { DiscoverViewModelFactory(repository) }
    val vm: DiscoverViewModel = viewModel(factory = factory)
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
    onOpenReader: (chapterTitle: String) -> Unit
) {
    val context = LocalContext.current

    // Skeleton: tạo database + repository local-first trực tiếp (chưa dùng DI/Hilt).
    val database = androidx.compose.runtime.remember(context) { AppDatabase.getInstance(context) }
    val repository = androidx.compose.runtime.remember(database) { LibraryRepository(database.libraryDao()) }
    val factory = androidx.compose.runtime.remember(repository) { LibraryViewModelFactory(repository) }

    val vm: LibraryViewModel = viewModel(factory = factory)
    val items by vm.libraryItems.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        items(items, key = { it.manga_id }) { item ->
            ListItem(
                headlineContent = { Text(text = item.title) },
                supportingContent = { Text(text = item.status.toDisplayName()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val chapterTitle = "${item.title} - Chapter ${item.last_read_chapter_id ?: "Mock"}"
                        onOpenReader(chapterTitle)
                    }
            )
        }
    }
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

private fun LibraryStatus.toDisplayName(): String = when (this) {
    LibraryStatus.READING -> "Đang đọc"
    LibraryStatus.COMPLETED -> "Đã đọc"
    LibraryStatus.FAVORITE -> "Yêu thích"
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

