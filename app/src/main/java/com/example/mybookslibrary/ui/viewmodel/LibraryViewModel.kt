package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel cho LibraryScreen — observe danh sách manga đã lưu trong Room DB
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repository: LibraryRepository,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _showFavoritesOnly = MutableStateFlow(false)
        val showFavoritesOnly = _showFavoritesOnly.asStateFlow()

        val libraryItems: Flow<List<LibraryItemEntity>> =
            repository.observeLibraryItems().combine(_showFavoritesOnly) { items, favoritesOnly ->
                if (favoritesOnly) items.filter { it.is_favorite } else items
            }

        fun setShowFavoritesOnly(favoritesOnly: Boolean) {
            _showFavoritesOnly.value = favoritesOnly
        }

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        fun refresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                delay(REFRESH_DELAY_MS)
                _isRefreshing.value = false
            }
        }

        fun removeBookmark(mangaId: String) {
            viewModelScope.launch(ioDispatcher) {
                repository.removeBookmark(mangaId)
            }
        }

        companion object {
            private const val REFRESH_DELAY_MS = 500L
        }
    }
