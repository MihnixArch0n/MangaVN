package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ViewModel cho LibraryScreen — observe danh sách manga đã lưu trong Room DB
@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: LibraryRepository
) : ViewModel() {
    val libraryItems: Flow<List<LibraryItemEntity>> = repository.observeLibraryItems()
}
