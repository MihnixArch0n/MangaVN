package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.dao.LibraryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ReadingHistoryViewModel
    @Inject
    constructor(
        libraryDao: LibraryDao,
    ) : ViewModel() {
        val historyItems: Flow<List<LibraryItemEntity>> = libraryDao.observeReadingHistory()
    }
