package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mybookslibrary.ui.theme.Dimens
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
    onReadingHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    vm: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val continueReading by vm.continueReading.collectAsStateWithLifecycle()
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
                onHistoryClick = onReadingHistoryClick,
                onSettingsClick = onSettingsClick,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.items.isNotEmpty(),
            onRefresh = { vm.loadDiscover() },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        ) {
            when {
                uiState.isLoading && uiState.items.isEmpty() ->
                    DiscoverLoadingState(Modifier.fillMaxSize())
                uiState.error != null && uiState.items.isEmpty() ->
                    DiscoverErrorState(
                        modifier = Modifier.fillMaxSize(),
                        onRetry = vm::loadDiscover,
                    )
                else ->
                    DiscoverContentList(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomNavPadding + Dimens.SpacingLg),
                    continueReading = continueReading,
                    onContinueReadingClick = { item ->
                        onMangaClick(
                            MangaModel(
                                id = item.manga_id,
                                title = item.title,
                                coverArt = item.cover_url,
                                description = "",
                                tags = emptyList(),
                            ),
                        )
                    },
                    onReadingHistoryClick = onReadingHistoryClick,
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
}
