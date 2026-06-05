package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
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

// ViewModel cho ReaderScreen — tải ảnh chapter và đồng bộ tiến độ đọc
@HiltViewModel
class ReaderViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val mangaRepository: MangaRepository,
    private val libraryRepository: LibraryRepository,
    private val downloadedChapterCache: DownloadedChapterCache,
    private val offlineDownloadStorage: OfflineDownloadStorage,
    private val tapZoneEvaluator: TapZoneEvaluator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AndroidViewModel(application) {

    private fun str(resId: Int) = getApplication<Application>().getString(resId)

    // Đọc nav args từ SavedStateHandle (key phải khớp với ReaderDestination)
    private val mangaId: String = savedStateHandle.get<String>(MANGA_ID_ARG).orEmpty()
    private val chapterId: String = savedStateHandle.get<String>(CHAPTER_ID_ARG).orEmpty()
    private val chapterTitleArg: String = savedStateHandle.get<String>(CHAPTER_TITLE_ARG).orEmpty()
    private val startPageIndexArg: Int = savedStateHandle.get<Int>(START_PAGE_INDEX_ARG) ?: 0
    // Tránh gọi Room update trùng lặp khi page index không thay đổi
    private var lastSyncedPageIndex: Int? = null
    private var pendingPageIndex: Int? = null

    private val _state = MutableStateFlow(
        ReaderState(
            chapterTitle = chapterTitleArg,
            lastReadPageIndex = startPageIndexArg
        )
    )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReaderUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ReaderUiEffect> = _effects.asSharedFlow()

    init {
        Timber.i(
            "ReaderViewModel init: mangaId=%s chapterId=%s chapterTitle=%s startPageIndex=%d",
            mangaId,
            chapterId,
            chapterTitleArg,
            startPageIndexArg
        )
        loadChapterPages()
    }

    // Ưu tiên trang offline nếu chapter đã tải; fallback At-Home API nếu chưa có bản local.
    private fun loadChapterPages() {
        if (chapterId.isEmpty()) {
            Timber.w("loadChapterPages aborted: missing chapterId")
            _state.update { it.copy(error = str(R.string.error_missing_chapter)) }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            Timber.d("loadChapterPages start: chapterId=%s", chapterId)
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val isDownloaded = downloadedChapterCache.isChapterDownloaded(chapterId)
                val localPages = if (isDownloaded) {
                    offlineDownloadStorage.getChapterPages(mangaId, chapterId)
                } else {
                    emptyList()
                }

                if (localPages.isNotEmpty()) {
                    val localPageUris = localPages.map { it.toUri().toString() }
                    Timber.d(
                        "loadChapterPages local success: mangaId=%s chapterId=%s pages=%d",
                        mangaId,
                        chapterId,
                        localPageUris.size
                    )
                    _state.update { it.copy(pages = localPageUris, isLoading = false, error = null) }
                    return@launch
                }

                if (isDownloaded) {
                    Timber.w("loadChapterPages local missing, fallback network: mangaId=%s chapterId=%s", mangaId, chapterId)
                }

                mangaRepository.getChapterPages(chapterId).onSuccess { urls ->
                    Timber.d("loadChapterPages success: chapterId=%s pages=%d", chapterId, urls.size)
                    if (urls.isEmpty()) {
                        _state.update { it.copy(pages = emptyList(), isLoading = false, error = str(R.string.error_load_pages)) }
                    } else {
                        _state.update { it.copy(pages = urls, isLoading = false, error = null) }
                    }
                }.onFailure { throwable ->
                    Timber.w(
                        throwable,
                        "loadChapterPages failed: chapterId=%s error=%s",
                        chapterId,
                        throwable.message ?: "<no-message>"
                    )
                    _state.update { it.copy(isLoading = false, error = throwable.message ?: str(R.string.error_load_pages)) }
                }
            } catch (t: Throwable) {
                Timber.e(t, "loadChapterPages crashed: chapterId=%s", chapterId)
                _state.update { it.copy(isLoading = false, error = t.message ?: str(R.string.error_unexpected)) }
            }
        }
    }

    fun onEvent(event: ReaderEvent) {
        when (event) {
            ReaderEvent.ToggleOverlay -> toggleOverlay()
            is ReaderEvent.ChangeReadingMode -> changeReadingMode(event.mode)
            ReaderEvent.CycleReadingMode -> cycleReadingMode()
            is ReaderEvent.JumpToPage -> jumpToPage(event.pageIndex)
            is ReaderEvent.VisiblePageChanged -> onVisiblePageChanged(event.pageIndex)
            is ReaderEvent.FlushProgress -> flushProgress(event.pageIndex)
            is ReaderEvent.TapOnScreen -> handleScreenTap(event)
            is ReaderEvent.PageLongPressed -> showPageActions(event.pageUrl, event.pageIndex)
            ReaderEvent.DismissPageActions -> dismissPageActions()
            is ReaderEvent.PageActionSelected -> handlePageActionSelected(event.action)
            is ReaderEvent.PageActionCompleted -> handlePageActionCompleted(event.action)
            is ReaderEvent.PageActionFailed -> handlePageActionFailed(event.action, event.message)
        }
    }

    private fun toggleOverlay() {
        _state.update { it.copy(isOverlayVisible = !it.isOverlayVisible) }
    }

    /**
     * Updates the current [ReadingMode] and logs the transition.
     */
    private fun changeReadingMode(mode: ReadingMode) {
        val oldMode = _state.value.currentReadingMode
        if (oldMode == mode) return
        Timber.d("ReadingMode changed: %s -> %s", oldMode, mode)
        _state.update { it.copy(currentReadingMode = mode) }
    }

    private fun cycleReadingMode() {
        val next = when (_state.value.currentReadingMode) {
            ReadingMode.VERTICAL -> ReadingMode.LTR
            ReadingMode.LTR -> ReadingMode.RTL
            ReadingMode.RTL -> ReadingMode.VERTICAL
        }
        changeReadingMode(next)
    }

    /**
     * Handles a [ReaderTapAction] from the tap zone system.
     * For NEXT/PREVIOUS, computes the target page index and emits it
     * via [effects] for the Pager to consume.
     */
    private fun navigateToPage(action: ReaderTapAction) {
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
        Timber.v("navigateToPage: action=%s from=%d to=%d", action, current.lastReadPageIndex, targetIndex)
        jumpToPage(targetIndex)
    }

    private fun jumpToPage(pageIndex: Int) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val targetIndex = pageIndex.coerceIn(0, pages.lastIndex)
        if (targetIndex == _state.value.lastReadPageIndex) return
        Timber.v("jumpToPage: from=%d to=%d", _state.value.lastReadPageIndex, targetIndex)
        _state.update { it.copy(lastReadPageIndex = targetIndex) }
        _effects.tryEmit(ReaderUiEffect.NavigateToPage(targetIndex))
    }

    private fun handleScreenTap(event: ReaderEvent.TapOnScreen) {
        val action = tapZoneEvaluator(
            x = event.x,
            y = event.y,
            screenWidth = event.width,
            screenHeight = event.height,
            mode = _state.value.currentReadingMode
        )
        navigateToPage(action)
    }

    /**
     * Receives the active page index from the UI layer.
     *
     * The UI already debounces noisy scroll updates, so this method only keeps the
     * latest index and persists it when it actually changes.
     */
    private fun onVisiblePageChanged(index: Int) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val boundedIndex = index.coerceIn(0, pages.lastIndex)
        pendingPageIndex = boundedIndex
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
    private fun flushProgress(index: Int?) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val target = (index ?: pendingPageIndex ?: _state.value.lastReadPageIndex)
            .coerceIn(0, pages.lastIndex)
        Timber.v(
            "flushProgress: requested=%s pending=%s target=%d",
            index?.toString() ?: "<none>",
            pendingPageIndex?.toString() ?: "<none>",
            target
        )
        if (target != _state.value.lastReadPageIndex) {
            _state.update { it.copy(lastReadPageIndex = target) }
        }
        syncProgressToRoom(pageIndexOverride = target, force = true)
    }

    private fun showPageActions(pageUrl: String, pageIndex: Int) {
        Timber.d("showPageActions: page=%d url=%s", pageIndex + 1, pageUrl)
        _state.update {
            it.copy(selectedPageActionTarget = ReaderPageActionTarget(pageUrl, pageIndex))
        }
    }

    private fun dismissPageActions() {
        _state.update { it.copy(selectedPageActionTarget = null) }
    }

    private fun handlePageActionSelected(action: ReaderPageAction) {
        val target = _state.value.selectedPageActionTarget ?: return
        val pageFile = buildPageFile(target)
        Timber.d("handlePageActionSelected: action=%s page=%d", action, target.pageIndex + 1)
        when (action) {
            ReaderPageAction.QuickSave -> _effects.tryEmit(
                ReaderUiEffect.QuickSavePage(target, pageFile.fileName)
            )
            ReaderPageAction.SaveAs -> _effects.tryEmit(
                ReaderUiEffect.SavePageAs(target, pageFile.fileName, pageFile.extension)
            )
            ReaderPageAction.Share -> _effects.tryEmit(
                ReaderUiEffect.SharePage(target, pageFile.fileName)
            )
        }
    }

    private fun handlePageActionCompleted(action: ReaderPageAction) {
        Timber.d("handlePageActionCompleted: action=%s", action)
        _effects.tryEmit(ReaderUiEffect.ShowPageActionResult(action = action))
    }

    private fun handlePageActionFailed(action: ReaderPageAction, message: String) {
        Timber.d("handlePageActionFailed: action=%s message=%s", action, message)
        _effects.tryEmit(ReaderUiEffect.ShowPageActionResult(action = action, errorMessage = message))
    }

    private fun buildPageFile(target: ReaderPageActionTarget): PageFile {
        val chapterSlug = _state.value.chapterTitle
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "chapter" }
        val extension = target.pageUrl
            .substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('/', "")
            .substringAfterLast('.', "jpg")
            .takeIf { it.isNotBlank() }
            ?: "jpg"
        val pageNumber = target.pageIndex + 1
        val fileName = "${chapterSlug}_p${pageNumber}_${target.pageUrl.hashCode().toUInt().toString(16)}"
        return PageFile(fileName = fileName, extension = extension)
    }

    // Lưu tiến độ đọc vào Room DB (chỉ cập nhật nếu manga đã có trong library)
    private fun syncProgressToRoom(pageIndexOverride: Int? = null, force: Boolean = false) {
        val pageIndex = pageIndexOverride ?: _state.value.lastReadPageIndex
        val totalPages = _state.value.pages.size
        if (!force && lastSyncedPageIndex == pageIndex) {
            Timber.v("syncProgressToRoom skipped: pageIndex=%d force=%s", pageIndex, force)
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

private data class PageFile(
    val fileName: String,
    val extension: String
)
