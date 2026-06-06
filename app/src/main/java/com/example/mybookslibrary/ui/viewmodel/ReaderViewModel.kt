package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.R
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.usecase.LoadReaderPagesUseCase
import com.example.mybookslibrary.domain.usecase.SyncReadingProgressUseCase
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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

@HiltViewModel
@Suppress("LongParameterList")
class ReaderViewModel
@Inject
constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val loadReaderPagesUseCase: LoadReaderPagesUseCase,
    private val syncReadingProgressUseCase: SyncReadingProgressUseCase,
    private val tapZoneEvaluator: TapZoneEvaluator,
    private val pageFileBuilder: ReaderPageFileBuilder,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AndroidViewModel(application) {
    private fun str(resId: Int) = getApplication<Application>().getString(resId)

    private val mangaId: String = savedStateHandle.get<String>(MANGA_ID_ARG).orEmpty()
    private val chapterId: String = savedStateHandle.get<String>(CHAPTER_ID_ARG).orEmpty()
    private val chapterTitleArg: String = savedStateHandle.get<String>(CHAPTER_TITLE_ARG).orEmpty()
    private val startPageIndexArg: Int = savedStateHandle.get<Int>(START_PAGE_INDEX_ARG) ?: 0

    private var lastSyncedPageIndex: Int? = null
    private var pendingPageIndex: Int? = null

    private val _state =
        MutableStateFlow(
            ReaderState(
                chapterTitle = chapterTitleArg,
                lastReadPageIndex = startPageIndexArg,
            ),
        )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReaderUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ReaderUiEffect> = _effects.asSharedFlow()

    init {
        loadChapterPages()
    }

    private fun loadChapterPages() {
        if (chapterId.isEmpty()) {
            Timber.w("loadChapterPages aborted: missing chapterId")
            _state.update { it.copy(error = str(R.string.error_missing_chapter)) }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            Timber.d("loadChapterPages start: chapterId=%s", chapterId)
            _state.update { it.copy(isLoading = true, error = null) }

            loadReaderPagesUseCase(mangaId, chapterId)
                .onSuccess { urls ->
                    Timber.d("loadChapterPages success: chapterId=%s pages=%d", chapterId, urls.size)
                    _state.update {
                        if (urls.isEmpty()) {
                            it.copy(
                                pages = emptyList(),
                                isLoading = false,
                                error = str(R.string.error_load_pages),
                            )
                        } else {
                            it.copy(pages = urls, isLoading = false, error = null)
                        }
                    }
                }.onFailure { throwable ->
                    Timber.w(throwable, "loadChapterPages failed: chapterId=%s", chapterId)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: str(R.string.error_load_pages),
                        )
                    }
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

    private fun changeReadingMode(mode: ReadingMode) {
        val oldMode = _state.value.currentReadingMode
        if (oldMode == mode) return
        Timber.d("ReadingMode changed: %s -> %s", oldMode, mode)
        _state.update { it.copy(currentReadingMode = mode) }
    }

    private fun cycleReadingMode() {
        changeReadingMode(_state.value.currentReadingMode.next())
    }

    private fun navigateToPage(action: ReaderTapAction) {
        val current = _state.value
        if (action == ReaderTapAction.TOGGLE_OVERLAY) {
            toggleOverlay()
            return
        }
        if (current.pages.isEmpty()) return

        val targetIndex =
            when (action) {
                ReaderTapAction.NEXT_PAGE -> (current.lastReadPageIndex + 1).coerceAtMost(current.pages.lastIndex)
                ReaderTapAction.PREVIOUS_PAGE -> (current.lastReadPageIndex - 1).coerceAtLeast(0)
                ReaderTapAction.NONE,
                ReaderTapAction.TOGGLE_OVERLAY,
                -> return
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
        val action =
            tapZoneEvaluator(
                x = event.x,
                y = event.y,
                screenWidth = event.width,
                screenHeight = event.height,
                mode = _state.value.currentReadingMode,
            )
        navigateToPage(action)
    }

    private fun onVisiblePageChanged(index: Int) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val boundedIndex = index.coerceIn(0, pages.lastIndex)
        pendingPageIndex = boundedIndex
        if (boundedIndex == _state.value.lastReadPageIndex) return
        _state.update { it.copy(lastReadPageIndex = boundedIndex) }
        syncProgressToRoom(pageIndexOverride = boundedIndex, force = false)
    }

    private fun flushProgress(index: Int?) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        val target =
            (index ?: pendingPageIndex ?: _state.value.lastReadPageIndex)
                .coerceIn(0, pages.lastIndex)
        if (target != _state.value.lastReadPageIndex) {
            _state.update { it.copy(lastReadPageIndex = target) }
        }
        syncProgressToRoom(pageIndexOverride = target, force = true)
    }

    private fun showPageActions(pageUrl: String, pageIndex: Int,) {
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
        val pageFile = pageFileBuilder(_state.value.chapterTitle, target)
        Timber.d("handlePageActionSelected: action=%s page=%d", action, target.pageIndex + 1)
        when (action) {
            ReaderPageAction.QuickSave ->
                _effects.tryEmit(
                    ReaderUiEffect.QuickSavePage(target, pageFile.fileName),
                )
            ReaderPageAction.SaveAs ->
                _effects.tryEmit(
                    ReaderUiEffect.SavePageAs(target, pageFile.fileName, pageFile.extension),
                )
            ReaderPageAction.Share ->
                _effects.tryEmit(
                    ReaderUiEffect.SharePage(target, pageFile.fileName),
                )
        }
    }

    private fun handlePageActionCompleted(action: ReaderPageAction) {
        Timber.d("handlePageActionCompleted: action=%s", action)
        _effects.tryEmit(ReaderUiEffect.ShowPageActionResult(action = action))
    }

    private fun handlePageActionFailed(action: ReaderPageAction, message: String,) {
        Timber.d("handlePageActionFailed: action=%s message=%s", action, message)
        _effects.tryEmit(ReaderUiEffect.ShowPageActionResult(action = action, errorMessage = message))
    }

    private fun syncProgressToRoom(pageIndexOverride: Int? = null, force: Boolean = false,) {
        val pageIndex = pageIndexOverride ?: _state.value.lastReadPageIndex
        val totalPages = _state.value.pages.size
        if (!force && lastSyncedPageIndex == pageIndex) {
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                syncReadingProgressUseCase(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    pageIndex = pageIndex,
                    totalPages = totalPages,
                )
                lastSyncedPageIndex = pageIndex
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

private fun ReadingMode.next(): ReadingMode = when (this) {
    ReadingMode.VERTICAL -> ReadingMode.LTR
    ReadingMode.LTR -> ReadingMode.RTL
    ReadingMode.RTL -> ReadingMode.VERTICAL
}
