package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.model.ReaderTapAction
import kotlinx.coroutines.CoroutineDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ReaderState(
    val chapterTitle: String = "",
    val pages: List<String> = emptyList(),
    val isOverlayVisible: Boolean = false,
    val lastReadPageIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentReadingMode: ReadingMode = ReadingMode.LTR
)

// ViewModel cho ReaderScreen — tải ảnh chapter và đồng bộ tiến độ đọc
@HiltViewModel
class ReaderViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val mangaRepository: MangaRepository,
    private val libraryRepository: LibraryRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AndroidViewModel(application) {

    private fun str(resId: Int) = getApplication<Application>().getString(resId)

    // Đọc nav args từ SavedStateHandle (key phải khớp với ReaderDestination)
    private val mangaId: String = savedStateHandle.get<String>(MANGA_ID_ARG).orEmpty()
    private val chapterId: String = savedStateHandle.get<String>(CHAPTER_ID_ARG).orEmpty()
    // Tránh gọi Room update trùng lặp khi page index không thay đổi
    private var lastSyncedPageIndex: Int? = null
    private var pendingPageIndex: Int? = null

    private val _state = MutableStateFlow(
        ReaderState(
            chapterTitle = savedStateHandle.get<String>(CHAPTER_TITLE_ARG).orEmpty(),
            lastReadPageIndex = savedStateHandle.get<Int>(START_PAGE_INDEX_ARG) ?: 0
        )
    )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    // One-shot event: target page index for HorizontalPager to animate to
    private val _pageNavigationEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val pageNavigationEvent: SharedFlow<Int> = _pageNavigationEvent.asSharedFlow()

    init {
        loadChapterPages()
    }

    // Gọi At-Home API để lấy URL ảnh từng trang của chapter
    private fun loadChapterPages() {
        if (chapterId.isEmpty()) {
            _state.update { it.copy(error = str(R.string.error_missing_chapter)) }
            return
        }

        viewModelScope.launch(ioDispatcher) {
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

    /**
     * Updates the current [ReadingMode] and logs the transition.
     */
    fun setReadingMode(mode: ReadingMode) {
        val oldMode = _state.value.currentReadingMode
        if (oldMode == mode) return
        Timber.d("ReadingMode changed: %s -> %s", oldMode, mode)
        _state.update { it.copy(currentReadingMode = mode) }
    }

    /**
     * Handles a [ReaderTapAction] from the tap zone system.
     * For NEXT/PREVIOUS, computes the target page index and emits it
     * via [pageNavigationEvent] for the Pager to consume.
     */
    fun navigateToPage(action: ReaderTapAction) {
        val current = _state.value
        if (action == ReaderTapAction.TOGGLE_OVERLAY) {
            toggleOverlay()
            return
        }
        if (current.pages.isEmpty()) return

        val targetIndex = when (action) {
            ReaderTapAction.NEXT_PAGE -> (current.lastReadPageIndex + 1).coerceAtMost(current.pages.lastIndex)
            ReaderTapAction.PREVIOUS_PAGE -> (current.lastReadPageIndex - 1).coerceAtLeast(0)
            ReaderTapAction.NONE -> return
            ReaderTapAction.TOGGLE_OVERLAY -> return
        }

        if (targetIndex == current.lastReadPageIndex) return
        Timber.d("navigateToPage: action=%s from=%d to=%d", action, current.lastReadPageIndex, targetIndex)
        _state.update { it.copy(lastReadPageIndex = targetIndex) }
        _pageNavigationEvent.tryEmit(targetIndex)
    }

    /**
     * Receives the active page index from the UI layer.
     *
     * The UI already debounces noisy scroll updates, so this method only keeps the
     * latest index and persists it when it actually changes.
     */
    fun onVisiblePageChanged(index: Int) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val boundedIndex = index.coerceIn(0, pages.lastIndex)
        pendingPageIndex = boundedIndex
        Timber.d("onVisiblePageChanged: index=%d bounded=%d total=%d", index, boundedIndex, pages.size)
        if (boundedIndex == _state.value.lastReadPageIndex) return
        _state.update { it.copy(lastReadPageIndex = boundedIndex) }
        syncProgressToRoom(pageIndexOverride = boundedIndex, force = false)
    }

    /**
     * Flushes the latest known page index immediately.
     *
     * This is used when the screen leaves before the debounce window finishes, so the
     * last visible page is not lost.
     */
    fun flushProgress(index: Int?) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val target = (index ?: pendingPageIndex ?: _state.value.lastReadPageIndex)
            .coerceIn(0, pages.lastIndex)
        Timber.d("flushProgress: requested=%s pending=%s target=%d", index?.toString() ?: "<none>", pendingPageIndex?.toString() ?: "<none>", target)
        if (target != _state.value.lastReadPageIndex) {
            _state.update { it.copy(lastReadPageIndex = target) }
        }
        syncProgressToRoom(pageIndexOverride = target, force = true)
    }

    // Lưu tiến độ đọc vào Room DB (chỉ cập nhật nếu manga đã có trong library)
    private fun syncProgressToRoom(pageIndexOverride: Int? = null, force: Boolean = false) {
        val pageIndex = pageIndexOverride ?: _state.value.lastReadPageIndex
        val totalPages = _state.value.pages.size
        if (!force && lastSyncedPageIndex == pageIndex) {
            Timber.d("syncProgressToRoom skipped: pageIndex=%d force=%s", pageIndex, force)
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                Timber.d(
                    "syncProgressToRoom start: mangaId=%s chapterId=%s pageIndex=%d totalPages=%d force=%s",
                    mangaId,
                    chapterId,
                    pageIndex,
                    totalPages,
                    force
                )
                libraryRepository.updateReadingProgress(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    pageIndex = pageIndex,
                    totalPages = totalPages
                )
                lastSyncedPageIndex = pageIndex
                Timber.d("syncProgressToRoom end: pageIndex=%d", pageIndex)
            } catch (t: Throwable) {
                Timber.e(t, "syncProgressToRoom error")
            }
        }
    }

    companion object {
        private const val MANGA_ID_ARG = "mangaId"
        private const val CHAPTER_ID_ARG = "chapterId"
        private const val CHAPTER_TITLE_ARG = "chapterTitle"
        private const val START_PAGE_INDEX_ARG = "startPageIndex"
    }
}
