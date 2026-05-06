package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderState(
    val chapterTitle: String = "",
    val pages: List<String> = emptyList(),
    val isOverlayVisible: Boolean = false,
    val lastReadPageIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel cho ReaderScreen — tải ảnh chapter và đồng bộ tiến độ đọc
@HiltViewModel
class ReaderViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val mangaRepository: MangaRepository,
    private val libraryRepository: LibraryRepository
) : AndroidViewModel(application) {

    private fun str(resId: Int) = getApplication<Application>().getString(resId)

    // Đọc nav args từ SavedStateHandle (key phải khớp với ReaderDestination)
    private val mangaId: String = savedStateHandle.get<String>(MANGA_ID_ARG).orEmpty()
    private val chapterId: String = savedStateHandle.get<String>(CHAPTER_ID_ARG).orEmpty()
    // Tránh gọi Room update trùng lặp khi page index không thay đổi
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

    // Gọi At-Home API để lấy URL ảnh từng trang của chapter
    private fun loadChapterPages() {
        if (chapterId.isEmpty()) {
            _state.update { it.copy(error = str(R.string.error_missing_chapter)) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                mangaRepository.getChapterPages(chapterId).onSuccess { urls ->
                    _state.update { it.copy(pages = urls, isLoading = false, error = null) }
                }.onFailure { throwable ->
                    _state.update { it.copy(isLoading = false, error = throwable.message ?: str(R.string.error_load_pages)) }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: str(R.string.error_unexpected)) }
            }
        }
    }

    // Bật/tắt overlay (top bar + bottom bar) khi tap vào màn hình
    fun toggleOverlay() {
        _state.update { it.copy(isOverlayVisible = !it.isOverlayVisible) }
    }

    // Cập nhật page index hiện tại và đồng bộ tiến độ vào Room trên mỗi lần chuyển trang
    fun onVisiblePageChanged(index: Int) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val boundedIndex = index.coerceIn(0, pages.lastIndex)
        if (boundedIndex == _state.value.lastReadPageIndex) return
        _state.update { it.copy(lastReadPageIndex = boundedIndex) }
        syncProgressToRoom()
    }

    // Lưu tiến độ đọc vào Room DB (chỉ cập nhật nếu manga đã có trong library)
    fun syncProgressToRoom() {
        val pageIndex = _state.value.lastReadPageIndex
        if (lastSyncedPageIndex == pageIndex) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.updateReadingProgress(mangaId = mangaId, chapterId = chapterId, pageIndex = pageIndex)
                lastSyncedPageIndex = pageIndex
            } catch (t: Throwable) {
                Log.e(TAG, "syncProgressToRoom error", t)
            }
        }
    }

    companion object {
        private const val TAG = "ReaderViewModel"
        private const val MANGA_ID_ARG = "mangaId"
        private const val CHAPTER_ID_ARG = "chapterId"
        private const val CHAPTER_TITLE_ARG = "chapterTitle"
        private const val START_PAGE_INDEX_ARG = "startPageIndex"
    }
}
