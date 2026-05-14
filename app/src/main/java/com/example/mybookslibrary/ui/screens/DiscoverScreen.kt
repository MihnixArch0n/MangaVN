package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModel

@Suppress("unused")
@Composable
fun DiscoverScreenContent(
    onMangaClick: (MangaModel) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val vm: DiscoverViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val expandedPopular = remember { androidx.compose.runtime.mutableStateOf(false) }
    val expandedNew = remember { androidx.compose.runtime.mutableStateOf(false) }
    val expandedExplore = remember { androidx.compose.runtime.mutableStateOf(false) }

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
                    Text(
                        appString(R.string.error_prefix, uiState.error ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (items.isNotEmpty()) {
                        item { SectionHeader(appString(R.string.section_spotlight)) }
                        item {
                            SpotlightCard(items.first(), { onMangaClick(items.first()) }, Modifier.padding(horizontal = 24.dp))
                        }
                    }
                    if (items.size > 1) {
                        item { Spacer(Modifier.height(32.dp)) }
                        item { SectionHeader(appString(R.string.section_popular), expandedPopular.value) { expandedPopular.value = !expandedPopular.value } }
                        if (expandedPopular.value) {
                            item { ExpandedBookGrid(popularItems, onMangaClick) }
                        } else {
                            item { HorizontalBookScroll(popularItems, onMangaClick) }
                        }
                    }
                    if (items.size > 6) {
                        item { Spacer(Modifier.height(32.dp)) }
                        item { SectionHeader(appString(R.string.section_new_releases), expandedNew.value) { expandedNew.value = !expandedNew.value } }
                        if (expandedNew.value) {
                            item { ExpandedBookGrid(newItems, onMangaClick) }
                        } else {
                            item { HorizontalBookScroll(newItems, onMangaClick) }
                        }
                    }
                    if (items.size > 11) {
                        item { Spacer(Modifier.height(32.dp)) }
                        item { SectionHeader(appString(R.string.section_explore), expandedExplore.value) { expandedExplore.value = !expandedExplore.value } }
                        if (expandedExplore.value) {
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
    var menuExpanded = remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                appString(R.string.brand_name),
                style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            Box {
                IconButton(onClick = { menuExpanded.value = true }) {
                    Icon(Icons.Filled.Menu, appString(R.string.cd_menu), tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = menuExpanded.value, onDismissRequest = { menuExpanded.value = false }) {
                    DropdownMenuItem(
                        text = { Text(appString(R.string.nav_library), style = MaterialTheme.typography.bodyLarge) },
                        onClick = { menuExpanded.value = false; onLibraryClick() },
                        leadingIcon = { Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text(appString(R.string.settings_title), style = MaterialTheme.typography.bodyLarge) },
                        onClick = { menuExpanded.value = false; onProfileClick() },
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
                    Icon(
                        Icons.Filled.Person,
                        appString(R.string.cd_profile),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
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
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
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
        modifier = modifier.fillMaxWidth().height(340.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(model = manga.coverArt, contentDescription = manga.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                        startY = 300f
                    )
                )
            )
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
                Text(
                    manga.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HorizontalBookScroll(items: List<MangaModel>, onItemClick: (MangaModel) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { manga -> MangaCardItem(manga, { onItemClick(manga) }) }
    }
}

@Composable
private fun ExpandedBookGrid(items: List<MangaModel>, onItemClick: (MangaModel) -> Unit) {
    val chunked = remember(items) { items.chunked(3) }
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { manga ->
                    MangaCardItem(manga, { onItemClick(manga) }, Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MangaCardItem(manga: MangaModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(120.dp).clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            AsyncImage(model = manga.coverArt, contentDescription = manga.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            manga.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (manga.tags.isNotEmpty()) {
            Text(
                manga.tags.first(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


