package com.example.mybookslibrary.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
) : ViewModel() {
    companion object {
        private const val TAG = "LibraryViewModel"
    }

    val libraryItems: Flow<List<LibraryItemEntity>> = repository
        .observeLibraryItems()
        .onStart {
            // Seed mocks trước khi emit data
            Log.d(TAG, "LibraryViewModel: Seeding mock data before emitting...")
            repository.seedMockIfEmpty()
        }

    init {
        Log.d(TAG, "LibraryViewModel: Created")
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

