package com.example.mybookslibrary.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.LibraryRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderState(
    val chapterTitle: String = "",
    val pages: List<String> = emptyList(),
    val isOverlayVisible: Boolean = false,
    val lastReadPageIndex: Int = 0
)

class ReaderViewModel(
    chapterTitle: String,
    private val mangaId: String,
    private val chapterId: String,
    initialPageIndex: Int,
    private val repository: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        ReaderState(
            chapterTitle = chapterTitle,
            lastReadPageIndex = initialPageIndex
        )
    )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(500)
            _state.update { current ->
                current.copy(
                    pages = buildMockPages()
                )
            }
        }
    }

    fun toggleOverlay() {
        _state.update { current ->
            current.copy(isOverlayVisible = !current.isOverlayVisible)
        }
    }

    fun onVisiblePageChanged(index: Int) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return

        val boundedIndex = index.coerceIn(0, pages.lastIndex)
        if (boundedIndex == _state.value.lastReadPageIndex) return

        _state.update { current ->
            current.copy(lastReadPageIndex = boundedIndex)
        }
        saveProgressToDataStore(boundedIndex)

        if (boundedIndex == pages.lastIndex) {
            syncProgressToRoom()
        }
    }

    fun syncProgressToRoom() {
        val pageIndex = _state.value.lastReadPageIndex
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.updateReadingProgress(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    pageIndex = pageIndex
                )
                Log.d(TAG, "syncProgressToRoom(mangaId=$mangaId, chapterId=$chapterId, pageIndex=$pageIndex)")
            } catch (t: Throwable) {
                throw t
            }
        }
    }

    private fun saveProgressToDataStore(index: Int) {
        Log.d(TAG, "saveProgressToDataStore(index=$index)")
    }

    private fun buildMockPages(): List<String> {
        val packageName = "com.example.mybookslibrary"
        val mockResIds = listOf(
            R.drawable.mock_l_img_1,
            R.drawable.mock_l_img_2,
            R.drawable.mock_l_img_3,
            R.drawable.mock_l_img_4,
            R.drawable.mock_l_img_5,
        )
        return mockResIds.map { resId ->
            "android.resource://$packageName/$resId"
        }
    }

    companion object {
        private const val TAG = "ReaderViewModel"
    }
}

class ReaderViewModelFactory(
    private val chapterTitle: String,
    private val mangaId: String,
    private val chapterId: String,
    private val initialPageIndex: Int,
    private val repository: LibraryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(
                chapterTitle = chapterTitle,
                mangaId = mangaId,
                chapterId = chapterId,
                initialPageIndex = initialPageIndex,
                repository = repository
            ) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
