package com.example.mybookslibrary.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    chapterTitle: String
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState(chapterTitle = chapterTitle))
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            delay(500)
            _state.update { current ->
                current.copy(
                    pages = buildMockPages(),
                    lastReadPageIndex = 0
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
        Log.d(TAG, "syncProgressToRoom(pageIndex=${_state.value.lastReadPageIndex})")
    }

    private fun saveProgressToDataStore(index: Int) {
        Log.d(TAG, "saveProgressToDataStore(index=$index)")
    }

    private fun buildMockPages(): List<String> {
        val drawableUri = "android.resource://com.example.mybookslibrary/${com.example.mybookslibrary.R.drawable.ic_launcher_foreground}"
        return List(8) { drawableUri }
    }

    companion object {
        private const val TAG = "ReaderViewModel"
    }
}

class ReaderViewModelFactory(
    private val chapterTitle: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(chapterTitle) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
