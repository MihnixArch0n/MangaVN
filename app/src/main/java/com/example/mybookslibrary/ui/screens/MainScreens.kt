package com.example.mybookslibrary.ui.screens

import androidx.compose.runtime.Composable
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.viewmodel.SearchViewModel
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel

@Composable
fun DiscoverScreen(
    onMangaClick: (MangaModel) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    DiscoverScreenContent(onMangaClick, onSearchClick, onLibraryClick, onProfileClick)
}

@Composable
fun SearchScreen(
    onMangaClick: (MangaModel) -> Unit = {},
    viewModel: SearchViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
    SearchScreenContent(onMangaClick, viewModel)
}

@Composable
fun LibraryScreen(
    onOpenDetail: (mangaId: String, title: String, coverUrl: String) -> Unit
) {
    LibraryScreenContent(onOpenDetail)
}

@Composable
fun SettingScreen(
    viewModel: SettingsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
    SettingScreenContent(viewModel)
}
