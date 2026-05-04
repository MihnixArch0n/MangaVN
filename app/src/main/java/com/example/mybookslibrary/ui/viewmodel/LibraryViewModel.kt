package com.example.mybookslibrary.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
) : ViewModel() {
    companion object {
        private const val TAG = "LibraryViewModel"
    }

    // Observe library directly; seeding runs once in init.
    val libraryItems: Flow<List<LibraryItemEntity>> = repository.observeLibraryItems()

    init {
        Log.d(TAG, "LibraryViewModel: Created")
        viewModelScope.launch {
            runCatching {
                repository.seedMockIfEmpty()
            }.onFailure { error ->
                Log.e(TAG, "LibraryViewModel: Failed to seed mock data", error)
            }
        }
    }

    fun onRemoveBookmark(mangaId: String) {
        viewModelScope.launch {
            runCatching {
                repository.removeBookmark(mangaId)
            }.onFailure { error ->
                Log.e(TAG, "LibraryViewModel: Failed to remove bookmark", error)
            }
        }
    }
}

class LibraryViewModelFactory(
    private val repository: LibraryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(repository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
