package com.example.mybookslibrary.ui.screens

import androidx.compose.runtime.Composable
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.viewmodel.SearchViewModel
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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
    viewModel: SearchViewModel = hiltViewModel()
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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    SettingScreenContent(viewModel)
}
