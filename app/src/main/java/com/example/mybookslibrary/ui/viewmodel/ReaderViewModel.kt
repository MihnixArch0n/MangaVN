package com.example.mybookslibrary.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ReaderState(
    val chapterTitle: String = "",
    val pages: List<String> = emptyList(),
    val isOverlayVisible: Boolean = false,
    val lastReadPageIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mangaRepository: MangaRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val mangaId: String = savedStateHandle.get<String>(MANGA_ID_ARG).orEmpty()
    private val chapterId: String = savedStateHandle.get<String>(CHAPTER_ID_ARG).orEmpty()
    private var lastSyncedPageIndex: Int? = null

    private val _state = MutableStateFlow(
        ReaderState(
            chapterTitle = savedStateHandle.get<String>(CHAPTER_TITLE_ARG).orEmpty(),
            lastReadPageIndex = savedStateHandle.get<Int>(START_PAGE_INDEX_ARG) ?: 0
        )
    )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    init {
        loadChapterPages()
    }

    private fun loadChapterPages() {
        if (chapterId.isEmpty()) {
            _state.update { it.copy(error = "Missing chapterId") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                mangaRepository.getChapterPages(chapterId).onSuccess { urls ->
                    _state.update { state ->
                        state.copy(
                            pages = urls,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d(TAG, "loadChapterPages: Loaded ${urls.size} pages for chapterId=$chapterId")
                }.onFailure { throwable ->
                    val errorMsg = throwable.message ?: "Failed to load chapter pages"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    Log.e(TAG, "loadChapterPages error: $errorMsg", throwable)
                }
            } catch (t: Throwable) {
                val errorMsg = t.message ?: "Unexpected error"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                Log.e(TAG, "loadChapterPages exception: $errorMsg", t)
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
        if (lastSyncedPageIndex == pageIndex) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.updateReadingProgress(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    pageIndex = pageIndex
                )
                lastSyncedPageIndex = pageIndex
                Log.d(TAG, "syncProgressToRoom(mangaId=$mangaId, chapterId=$chapterId, pageIndex=$pageIndex)")
            } catch (t: Throwable) {
                Log.e(TAG, "syncProgressToRoom error", t)
            }
        }
    }

    private fun saveProgressToDataStore(index: Int) {
        Log.d(TAG, "saveProgressToDataStore(index=$index)")
    }

    companion object {
        private const val TAG = "ReaderViewModel"
        private const val MANGA_ID_ARG = "mangaId"
        private const val CHAPTER_ID_ARG = "chapterId"
        private const val CHAPTER_TITLE_ARG = "chapterTitle"
        private const val START_PAGE_INDEX_ARG = "startPageIndex"
    }
}

