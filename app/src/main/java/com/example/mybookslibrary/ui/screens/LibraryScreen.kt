package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.ui.theme.KansoDarkBackground
import com.example.mybookslibrary.ui.theme.KansoDarkSuccess
import com.example.mybookslibrary.ui.theme.KansoDarkWarning
import com.example.mybookslibrary.ui.theme.KansoSuccess
import com.example.mybookslibrary.ui.theme.KansoWarning
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModel

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    onOpenDetail: (mangaId: String, title: String, coverUrl: String) -> Unit
) {
    val vm: LibraryViewModel = hiltViewModel()
    val items by vm.libraryItems.collectAsStateWithLifecycle(initialValue = emptyList())
    var pendingRemoval by remember { mutableStateOf<LibraryItemEntity?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(appString(R.string.library_empty_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(appString(R.string.library_empty_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(appString(R.string.library_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                items(items, key = { it.manga_id }) { item ->
                    LibraryItemCard(
                        title = item.title,
                        coverUrl = item.cover_url,
                        status = item.status,
                        onClick = { onOpenDetail(item.manga_id, item.title, item.cover_url) },
                        onLongClick = { pendingRemoval = item }
                    )
                }
            }
        }

        if (pendingRemoval != null) {
            ModalBottomSheet(onDismissRequest = { pendingRemoval = null }) {
                val item = pendingRemoval ?: return@ModalBottomSheet
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(text = appString(R.string.library_remove_bookmark), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(text = appString(R.string.library_remove_bookmark_confirm, item.title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = {
                                vm.removeBookmark(item.manga_id)
                                pendingRemoval = null
                            }
                        ) { Text(appString(R.string.library_remove_bookmark)) }
                        TextButton(onClick = { pendingRemoval = null }) {
                            Text(appString(R.string.action_cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    title: String,
    coverUrl: String,
    status: LibraryStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(Modifier.size(60.dp, 90.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                AsyncImage(model = coverUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                StatusChip(status)
            }
        }
    }
}

@Composable
private fun StatusChip(status: LibraryStatus) {
    val isDark = MaterialTheme.colorScheme.background == KansoDarkBackground
    val label = when (status) {
        LibraryStatus.READING -> appString(R.string.status_reading)
        LibraryStatus.COMPLETED -> appString(R.string.status_completed)
        LibraryStatus.FAVORITE -> appString(R.string.status_favorite)
    }
    val color = when (status) {
        LibraryStatus.READING -> MaterialTheme.colorScheme.tertiary
        LibraryStatus.COMPLETED -> if (isDark) KansoDarkSuccess else KansoSuccess
        LibraryStatus.FAVORITE -> if (isDark) KansoDarkWarning else KansoWarning
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}


