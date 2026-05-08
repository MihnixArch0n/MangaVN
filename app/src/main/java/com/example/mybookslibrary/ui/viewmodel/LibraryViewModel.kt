package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

// ViewModel cho LibraryScreen — observe danh sách manga đã lưu trong Room DB
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {
    val libraryItems: Flow<List<LibraryItemEntity>> = repository.observeLibraryItems()

    fun removeBookmark(mangaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBookmark(mangaId)
        }
    }
}
