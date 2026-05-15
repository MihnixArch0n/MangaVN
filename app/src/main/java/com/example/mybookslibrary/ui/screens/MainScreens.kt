package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.composed
import com.example.mybookslibrary.ui.navigation.LocalNavAnimatedVisibilityScope
import com.example.mybookslibrary.ui.navigation.LocalSharedTransitionScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.theme.KansoDarkSuccess
import com.example.mybookslibrary.ui.theme.KansoDarkWarning
import com.example.mybookslibrary.ui.theme.KansoSuccess
import com.example.mybookslibrary.ui.theme.KansoWarning
import com.example.mybookslibrary.ui.viewmodel.BackupRestoreResult
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModel
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedCoverBounds(mangaId: String): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            this@composed.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "cover_$mangaId"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        this@composed
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Khám phá
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DiscoverScreen(
    onMangaClick: (MangaModel) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val vm: DiscoverViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    var expandedPopular by remember { mutableStateOf(false) }
    var expandedNew by remember { mutableStateOf(false) }
    var expandedExplore by remember { mutableStateOf(false) }

    val items = uiState.items
    val popularItems = remember(items) { if (items.size > 1) items.drop(1).take(5) else emptyList() }
    val newItems = remember(items) { if (items.size > 6) items.drop(6).take(5) else emptyList() }
    val exploreItems = remember(items) { if (items.size > 11) items.drop(11) else emptyList() }

    Scaffold(
        topBar = { EditorialTopBar(onSearchClick, onLibraryClick, onProfileClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(appString(R.string.error_prefix, uiState.error ?: ""),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (items.isNotEmpty()) {
                        item { SectionHeader(appString(R.string.section_spotlight)) }
                        item {
                            SpotlightCard(items.first(), { onMangaClick(items.first()) }, Modifier.padding(horizontal = 24.dp))
                        }
                    }
                    if (items.size > 1) {
                        item { Spacer(Modifier.height(32.dp)) }
                        item { SectionHeader(appString(R.string.section_popular), expandedPopular) { expandedPopular = !expandedPopular } }
                        if (expandedPopular) {
                            item { ExpandedBookGrid(popularItems, onMangaClick) }
                        } else {
                            item { HorizontalBookScroll(popularItems, onMangaClick) }
                        }
                    }
                    if (items.size > 6) {
                        item { Spacer(Modifier.height(32.dp)) }
                        item { SectionHeader(appString(R.string.section_new_releases), expandedNew) { expandedNew = !expandedNew } }
                        if (expandedNew) {
                            item { ExpandedBookGrid(newItems, onMangaClick) }
                        } else {
                            item { HorizontalBookScroll(newItems, onMangaClick) }
                        }
                    }
                    if (items.size > 11) {
                        item { Spacer(Modifier.height(32.dp)) }
                        item { SectionHeader(appString(R.string.section_explore), expandedExplore) { expandedExplore = !expandedExplore } }
                        if (expandedExplore) {
                            item { ExpandedBookGrid(exploreItems, onMangaClick) }
                        } else {
                            item { HorizontalBookScroll(exploreItems, onMangaClick) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorialTopBar(
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(appString(R.string.brand_name),
                style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.primary)
        },
        navigationIcon = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.Menu, appString(R.string.cd_menu), tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(appString(R.string.nav_library), style = MaterialTheme.typography.bodyLarge) },
                        onClick = { menuExpanded = false; onLibraryClick() },
                        leadingIcon = { Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(appString(R.string.settings_title), style = MaterialTheme.typography.bodyLarge) },
                        onClick = { menuExpanded = false; onProfileClick() },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, appString(R.string.cd_search), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, appString(R.string.cd_profile),
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun SectionHeader(title: String, expanded: Boolean = false, onToggle: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        if (onToggle != null) {
            Text(
                if (expanded) appString(R.string.action_collapse) else appString(R.string.action_see_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.clickable(onClick = onToggle)
            )
        }
    }
}

@Composable
private fun SpotlightCard(manga: MangaModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(340.dp).sharedCoverBounds(manga.id),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.coverArt)
                    .memoryCacheKey("cover_${manga.id}")
                    .build(),
                contentDescription = manga.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)), startY = 300f)
            ))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                if (manga.tags.isNotEmpty()) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(manga.tags.first().uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(manga.title, style = MaterialTheme.typography.headlineLarge, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HorizontalBookScroll(items: List<MangaModel>, onItemClick: (MangaModel) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { manga -> BookCoverCard(manga, { onItemClick(manga) }) }
    }
}

@Composable
private fun ExpandedBookGrid(items: List<MangaModel>, onItemClick: (MangaModel) -> Unit) {
    // Hiển thị dạng grid 3 cột khi mở rộng
    val chunked = remember(items) { items.chunked(3) }
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { manga ->
                    BookCoverCard(manga, { onItemClick(manga) }, Modifier.weight(1f))
                }
                // Placeholder cho hàng thiếu
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun BookCoverCard(manga: MangaModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(120.dp).clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp).sharedCoverBounds(manga.id),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.coverArt)
                    .memoryCacheKey("cover_${manga.id}")
                    .build(),
                contentDescription = manga.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(manga.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (manga.tags.isNotEmpty()) {
            Text(manga.tags.first(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Tìm kiếm
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SearchScreen(
    onMangaClick: (MangaModel) -> Unit = {},
    viewModel: com.example.mybookslibrary.ui.viewmodel.SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(appString(R.string.search_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text(appString(R.string.search_placeholder), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(appString(R.string.search_error_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(appString(R.string.search_error_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
                uiState.query.length < 2 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(appString(R.string.search_prompt_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(appString(R.string.search_prompt_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
                uiState.results.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(appString(R.string.search_no_results, uiState.query), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.results, key = { it.id }) { manga ->
                            SearchResultItem(manga) { onMangaClick(manga) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(manga: MangaModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Card(Modifier.size(56.dp, 84.dp).sharedCoverBounds(manga.id), shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(manga.coverArt)
                        .memoryCacheKey("cover_${manga.id}")
                        .build(),
                    contentDescription = manga.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(manga.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (manga.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(manga.tags.take(3).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Thư viện
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenDetail: (mangaId: String, title: String, coverUrl: String) -> Unit
) {
    val vm: LibraryViewModel = hiltViewModel()
    val items by vm.libraryItems.collectAsState(initial = emptyList())
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
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(appString(R.string.library_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                items(items, key = { it.manga_id }) { item ->
                    LibraryItemCard(
                        mangaId = item.manga_id,
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
                    Text(
                        text = appString(R.string.library_remove_bookmark),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = appString(R.string.library_remove_bookmark_confirm, item.title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    mangaId: String,
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
            Card(Modifier.size(60.dp, 90.dp).sharedCoverBounds(mangaId), shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .memoryCacheKey("cover_$mangaId")
                        .build(),
                    contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
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
    val isDark = MaterialTheme.colorScheme.background == com.example.mybookslibrary.ui.theme.KansoDarkBackground
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
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Cài đặt
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SettingScreen(
    viewModel: com.example.mybookslibrary.ui.viewmodel.SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.let { viewModel.backupLibrary(it) }
    }
    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.let { viewModel.restoreLibrary(it) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
        ) {
            item {
                Text(appString(R.string.settings_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(32.dp))
            }

            // Giao diện & Ngôn ngữ
            item { SettingsSectionLabel(appString(R.string.settings_section_appearance)) }
            item {
                val themeLabel = when (uiState.themeMode) {
                    "light" -> appString(R.string.settings_theme_light)
                    "dark" -> appString(R.string.settings_theme_dark)
                    else -> appString(R.string.settings_theme_system)
                }
                val langLabel = if (uiState.language == "vi") appString(R.string.settings_language_vietnamese) else appString(R.string.settings_language_english)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_dark_mode), themeLabel) { viewModel.cycleThemeMode() }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_language), langLabel) {
                        viewModel.setLanguage(if (uiState.language == "vi") "en" else "vi")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Đọc truyện
            item { SettingsSectionLabel(appString(R.string.settings_section_reading)) }
            item {
                val qualityLabel = if (uiState.quality == "data") appString(R.string.settings_quality_original) else appString(R.string.settings_quality_data_saver)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_image_quality), qualityLabel) { viewModel.toggleQuality() }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Bộ nhớ
            item { SettingsSectionLabel(appString(R.string.settings_section_storage)) }
            item {
                val cacheSubtitle = if (uiState.cacheCleared) appString(R.string.settings_cache_cleared) else appString(R.string.settings_cache_subtitle)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_clear_cache), cacheSubtitle) { viewModel.clearImageCache() }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Dữ liệu
            item { SettingsSectionLabel(appString(R.string.settings_section_data)) }
            item {
                val backupSub = when (val r = uiState.backupResult) {
                    is BackupRestoreResult.Success -> appString(R.string.settings_backup_success, r.count)
                    is BackupRestoreResult.Failure -> appString(R.string.settings_backup_failed, r.message)
                    null -> appString(R.string.settings_backup_subtitle)
                }
                val restoreSub = when (val r = uiState.restoreResult) {
                    is BackupRestoreResult.Success -> appString(R.string.settings_restore_success, r.count)
                    is BackupRestoreResult.Failure -> appString(R.string.settings_restore_failed, r.message)
                    null -> appString(R.string.settings_restore_subtitle)
                }
                SettingsCard {
                    SettingsRow(appString(R.string.settings_backup), backupSub) { backupLauncher.launch("kanso_library_backup.json") }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_restore), restoreSub) { restoreLauncher.launch(arrayOf("application/json")) }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Tài khoản
            item { SettingsSectionLabel(appString(R.string.settings_section_account)) }
            item {
                val signOutTitle = if (uiState.signedOut) appString(R.string.settings_signed_out) else appString(R.string.settings_sign_out)
                val signOutSub = if (uiState.signedOut) appString(R.string.settings_signed_out_subtitle) else appString(R.string.settings_sign_out_subtitle)
                SettingsCard {
                    SettingsRow(signOutTitle, signOutSub, MaterialTheme.colorScheme.tertiary) { viewModel.signOut() }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 20.dp)
        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
}
