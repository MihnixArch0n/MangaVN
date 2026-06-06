package com.example.mybookslibrary.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.viewmodel.SearchViewModel
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel

@Composable
fun DiscoverScreen(
    onMangaClick: (MangaModel) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    DiscoverScreenContent(
        onMangaClick = onMangaClick,
        onSearchClick = onSearchClick,
        onLibraryClick = onLibraryClick,
        onProfileClick = onProfileClick,
    )
}

@Composable
fun SearchScreen(onMangaClick: (MangaModel) -> Unit = {}, viewModel: SearchViewModel = hiltViewModel(),) {
    SearchScreenContent(
        onMangaClick = onMangaClick,
        viewModel = viewModel,
    )
}

@Composable
fun LibraryScreen(onOpenDetail: (mangaId: String, title: String, coverUrl: String) -> Unit) {
    LibraryScreenContent(onOpenDetail = onOpenDetail)
}

@Composable
fun SettingScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    SettingScreenContent(viewModel = viewModel)
}
