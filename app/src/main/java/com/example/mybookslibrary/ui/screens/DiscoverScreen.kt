package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.navigation.LocalBottomNavPadding
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModel

@Suppress("unused", "LongMethod", "LongParameterList")
@Composable
fun DiscoverScreenContent(
    modifier: Modifier = Modifier,
    onMangaClick: (MangaModel) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    vm: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val expandedPopular = remember { mutableStateOf(false) }
    val expandedNew = remember { mutableStateOf(false) }
    val expandedExplore = remember { mutableStateOf(false) }
    val bottomNavPadding = LocalBottomNavPadding.current

    val items = uiState.items
    val popularItems = remember(items) { if (items.size > 1) items.drop(1).take(5) else emptyList() }
    val newItems = remember(items) { if (items.size > 6) items.drop(6).take(5) else emptyList() }
    val exploreItems = remember(items) { if (items.size > 11) items.drop(11) else emptyList() }

    Scaffold(
        modifier = modifier,
        topBar = {
            EditorialTopBar(
                onSearchClick = onSearchClick,
                onLibraryClick = onLibraryClick,
                onProfileClick = onProfileClick,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                DiscoverLoadingState(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            uiState.error != null ->
                DiscoverErrorState(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                    onRetry = vm::loadDiscover,
                )
            else ->
                DiscoverContentList(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                    contentPadding = PaddingValues(bottom = bottomNavPadding + 16.dp),
                    spotlight = items.firstOrNull(),
                    popularItems = popularItems,
                    newItems = newItems,
                    exploreItems = exploreItems,
                    expandedPopular = expandedPopular.value,
                    expandedNew = expandedNew.value,
                    expandedExplore = expandedExplore.value,
                    onTogglePopular = { expandedPopular.value = !expandedPopular.value },
                    onToggleNew = { expandedNew.value = !expandedNew.value },
                    onToggleExplore = { expandedExplore.value = !expandedExplore.value },
                    onMangaClick = onMangaClick,
                )
        }
    }
}
