package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.domain.model.ReaderBackground
import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.usecase.LoadReaderPagesUseCase
import com.example.mybookslibrary.domain.usecase.SyncReadingProgressUseCase
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
import com.example.mybookslibrary.ui.navigation.Reader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val chapterDao: ChapterDao,
    private val tapZoneEvaluator: TapZoneEvaluator,
    private val pageFileBuilder: ReaderPageFileBuilder,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val imageLoader: ImageLoader,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val offlineDownloadRepository: OfflineDownloadRepository,
    private val downloadedChapterCache: DownloadedChapterCache,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AndroidViewModel(application) {
    private fun str(resId: Int) = getApplication<Application>().getString(resId)

    private val route: Reader = savedStateHandle.toRoute()
    private val mangaId: String = route.mangaId
    private val chapterId: String = route.chapterId
    private val chapterTitleArg: String = route.chapterTitle
    private val startPageIndexArg: Int = route.startPageIndex

    private var lastSyncedPageIndex: Int? = null
    private var pendingPageIndex: Int? = null

    @Volatile
    private var readingModeChangedByUser = false

    // Seamless navigation (Phase 4 PR-2a): job auto-advance đang chờ + cờ đã preload chương kế.
    private var autoAdvanceJob: Job? = null
    private var preloadedNextChapterId: String? = null

    // Auto-download (Phase 4 PR-2b): cờ đã trigger tải chương kế, chống enqueue lặp khi lướt qua lại near-end.
    private var autoDownloadTriggeredForChapter: String? = null

    // Delete-after-read (Phase 4 PR-3b): chỉ dọn 1 lần/chương khi đọc xong.
    private var deleteAfterReadDoneForChapter: String? = null

    private val _state =
        MutableStateFlow(
            ReaderState(
                chapterTitle = chapterTitleArg,
                lastReadPageIndex = startPageIndexArg,
                isLoading = true,
            ),
        )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReaderUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ReaderUiEffect> = _effects.asSharedFlow()

    init {
        loadReadingMode()
        observeComfortPreferences()
    }

    // Quan sát các tuỳ chọn reader reactive: đổi trong Settings/panel là reader cập nhật ngay.
    // Tách 2 combine 3-flow vì combine có overload type-safe tối đa 5 flow (giờ có 6 pref).
    private fun observeComfortPreferences() {
        val displayPrefs =
            combine(
                userPreferencesDataStore.observeReaderKeepScreenOn(),
                userPreferencesDataStore.observeReaderVolumeKeyNav(),
                userPreferencesDataStore.observeReaderBrightness(),
            ) { keepScreenOn, volumeKeyNav, brightness -> Triple(keepScreenOn, volumeKeyNav, brightness) }

        val seamlessPrefs =
            combine(
                userPreferencesDataStore.observeReaderBackground(),
                userPreferencesDataStore.observeReaderAutoAdvance(),
                userPreferencesDataStore.observeAutoDownloadNext(),
            ) { background, autoAdvance, autoDownloadNext -> Triple(background, autoAdvance, autoDownloadNext) }

        combine(displayPrefs, seamlessPrefs) { display, seamless ->
            val (keepScreenOn, volumeKeyNav, brightness) = display
            val (background, autoAdvance, autoDownloadNext) = seamless
            ComfortPrefs(
                keepScreenOn = keepScreenOn,
                volumeKeyNav = volumeKeyNav,
                brightness = brightness,
                background = ReaderBackground.fromString(background),
                autoAdvance = autoAdvance,
                autoDownloadNext = autoDownloadNext,
            )
        }.onEach { prefs ->
            _state.update {
                it.copy(
                    keepScreenOn = prefs.keepScreenOn,
                    volumeKeyNav = prefs.volumeKeyNav,
                    brightness = prefs.brightness,
                    background = prefs.background,
                    autoAdvance = prefs.autoAdvance,
                    autoDownloadNext = prefs.autoDownloadNext,
                )
            }
        }.launchIn(viewModelScope)
    }

    private data class ComfortPrefs(
        val keepScreenOn: Boolean,
        val volumeKeyNav: Boolean,
        val brightness: Float,
        val background: ReaderBackground,
        val autoAdvance: Boolean,
        val autoDownloadNext: Boolean,
    )

    private fun loadReadingMode() {
        viewModelScope.launch(ioDispatcher) {
            try {
                // Per-manga override (Phase 4 PR-3a) thắng; thiếu thì fallback global default.
                val storedMode = userPreferencesDataStore.getReaderModeForManga(mangaId)
                    ?: userPreferencesDataStore.getReaderReadingMode()
                if (!readingModeChangedByUser) {
                    _state.update { it.copy(currentReadingMode = storedMode) }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Timber.e(t, "loadReadingMode error")
            }
            // Resolve the persisted direction before pages can compose. Loading local files is
            // often fast enough to otherwise build a pager in LTR and switch it to RTL while
            // its pointer gesture is already active.
            loadChapterPages()
        }
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
                    loadAdjacentChapters()
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
            ReaderEvent.RetryLoadPages -> loadChapterPages()
            ReaderEvent.ToggleOverlay -> toggleOverlay()
            ReaderEvent.ToggleComfortPanel ->
                _state.update { it.copy(isComfortPanelVisible = !it.isComfortPanelVisible) }
            is ReaderEvent.SetBrightness -> setBrightness(event.value)
            ReaderEvent.CommitBrightness -> commitBrightness()
            is ReaderEvent.SetBackground -> setBackground(event.background)
            ReaderEvent.VolumeKeyNextPage -> navigateToPage(ReaderTapAction.NEXT_PAGE)
            ReaderEvent.VolumeKeyPrevPage -> navigateToPage(ReaderTapAction.PREVIOUS_PAGE)
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
            ReaderEvent.NavigatePrevChapter ->
                navigateToChapter(_state.value.prevChapterId, _state.value.prevChapterTitle)
            ReaderEvent.NavigateNextChapter ->
                navigateToChapter(_state.value.nextChapterId, _state.value.nextChapterTitle)
            ReaderEvent.ReachedTransitionPage -> onReachedTransitionPage()
            ReaderEvent.LeftTransitionPage -> autoAdvanceJob?.cancel()
        }
    }

    // Tới trang chuyển tiếp cuối chương: preload chương kế (luôn) + nếu bật autoAdvance thì
    // hẹn chuyển sau AUTO_ADVANCE_DELAY_MS (lướt ngược lại sẽ hủy qua LeftTransitionPage).
    private fun onReachedTransitionPage() {
        preloadNextChapter()
        maybeAutoDownloadNextChapter()
        val nextId = _state.value.nextChapterId ?: return
        if (!_state.value.autoAdvance) return
        autoAdvanceJob?.cancel()
        autoAdvanceJob =
            viewModelScope.launch {
                delay(AUTO_ADVANCE_DELAY_MS)
                navigateToChapter(nextId, _state.value.nextChapterTitle)
            }
    }

    // Warm Coil cache vài ảnh đầu chương kế để mở chương sau gần như tức thì. Chỉ chạy 1 lần/chương.
    private fun preloadNextChapter() {
        val nextId = _state.value.nextChapterId ?: return
        if (preloadedNextChapterId == nextId) return
        preloadedNextChapterId = nextId
        viewModelScope.launch(ioDispatcher) {
            try {
                loadReaderPagesUseCase(mangaId, nextId)
                    .onSuccess { urls ->
                        val warmCount = urls.size.coerceAtMost(PRELOAD_IMAGE_COUNT)
                        urls.take(PRELOAD_IMAGE_COUNT).forEach { url ->
                            imageLoader.enqueue(
                                ImageRequest.Builder(getApplication()).data(url).build(),
                            )
                        }
                        Timber.d("preloadNextChapter warmed %d images: chapterId=%s", warmCount, nextId)
                    }.onFailure { t ->
                        Timber.d(t, "preloadNextChapter load failed (bỏ qua, best-effort): chapterId=%s", nextId)
                    }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Timber.e(t, "preloadNextChapter error: chapterId=%s", nextId)
            }
        }
    }

    // Tự tải chương kế về offline khi bật toggle. Chỉ trigger 1 lần/chương; bỏ qua nếu chương kế
    // đã tải xong hoặc đang trong queue. Network constraint (wifi-only) do enqueueDownload tự đọc.
    private fun maybeAutoDownloadNextChapter() {
        if (!_state.value.autoDownloadNext) return
        val nextId = _state.value.nextChapterId ?: return
        if (autoDownloadTriggeredForChapter == nextId) return
        autoDownloadTriggeredForChapter = nextId
        val nextLabel = _state.value.nextChapterTitle
        viewModelScope.launch(ioDispatcher) {
            try {
                if (downloadedChapterCache.isChapterDownloaded(nextId)) {
                    Timber.d("maybeAutoDownloadNextChapter: chương kế đã tải, bỏ qua: chapterId=%s", nextId)
                    return@launch
                }
                if (offlineDownloadRepository.getQueueByChapter(nextId) != null) {
                    Timber.d("maybeAutoDownloadNextChapter: chương kế đã trong queue, bỏ qua: chapterId=%s", nextId)
                    return@launch
                }
                Timber.d("maybeAutoDownloadNextChapter: enqueue chương kế: chapterId=%s", nextId)
                offlineDownloadManager.enqueueDownload(mangaId, nextId, chapterLabel = nextLabel)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Timber.e(t, "maybeAutoDownloadNextChapter error: chapterId=%s", nextId)
            }
        }
    }

    // Đọc xong chương → nếu bật delete-after-read, xóa file download của các chương đã đọc xong + đã tải,
    // giữ N chương mới nhất (theo feed_order). KHÔNG đụng chương hiện tại / chưa đọc / chưa tải.
    private fun maybeDeleteAfterRead() {
        if (deleteAfterReadDoneForChapter == chapterId) return
        deleteAfterReadDoneForChapter = chapterId
        viewModelScope.launch(ioDispatcher) {
            try {
                if (!userPreferencesDataStore.getDeleteAfterRead()) return@launch
                val keep = userPreferencesDataStore.getDeleteAfterReadKeep()
                val downloaded = downloadedChapterCache.downloadedChapterIds.value
                // Chương đã đọc xong + đã tải, theo feed_order (cũ -> mới).
                val completedDownloaded = chapterDao.getCompletedChapterIdsOrdered(mangaId)
                    .filter { it in downloaded }
                if (completedDownloaded.size <= keep) return@launch
                // Giữ N mới nhất (cuối list); xóa phần cũ hơn, chừa chương đang đọc.
                val toDelete = completedDownloaded.dropLast(keep).filter { it != chapterId }
                toDelete.forEach { id -> offlineDownloadManager.deleteDownload(mangaId, id) }
                Timber.d("deleteAfterRead: xóa %d chương cũ, giữ %d (manga=%s)", toDelete.size, keep, mangaId)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Timber.e(t, "maybeDeleteAfterRead error: manga=%s", mangaId)
            }
        }
    }

    private fun toggleOverlay() {
        _state.update { it.copy(isOverlayVisible = !it.isOverlayVisible) }
    }

    private fun changeReadingMode(mode: ReadingMode) {
        val oldMode = _state.value.currentReadingMode
        if (oldMode == mode) return
        Timber.d("ReadingMode changed: %s -> %s", oldMode, mode)
        readingModeChangedByUser = true
        _state.update { it.copy(currentReadingMode = mode) }
        _effects.tryEmit(ReaderUiEffect.ReadingModeChanged(mode))
        viewModelScope.launch(ioDispatcher) {
            try {
                // Lưu override cho truyện hiện tại + cập nhật global default (last-used cho truyện mới).
                userPreferencesDataStore.setReaderModeForManga(mangaId, mode)
                userPreferencesDataStore.setReaderReadingMode(mode)
            } catch (t: Throwable) {
                Timber.e(t, "saveReadingMode error: mode=%s", mode)
            }
        }
    }

    private fun cycleReadingMode() {
        changeReadingMode(_state.value.currentReadingMode.next())
    }

    // Chỉ cập nhật state (preview mượt khi kéo). Ghi DataStore tách qua commitBrightness khi thả slider.
    private fun setBrightness(value: Float) {
        val coerced = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        _state.update { it.copy(brightness = coerced) }
    }

    private fun commitBrightness() {
        val value = _state.value.brightness
        viewModelScope.launch(ioDispatcher) {
            try {
                userPreferencesDataStore.setReaderBrightness(value)
            } catch (t: Throwable) {
                Timber.e(t, "commitBrightness error: value=%f", value)
            }
        }
    }

    private fun setBackground(background: ReaderBackground) {
        _state.update { it.copy(background = background) }
        viewModelScope.launch(ioDispatcher) {
            try {
                userPreferencesDataStore.setReaderBackground(background.name)
            } catch (t: Throwable) {
                Timber.e(t, "setBackground error: background=%s", background)
            }
        }
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
        // Đọc gần cuối → preload chương kế sớm (warm Coil) cho mượt khi chuyển + auto-download nếu bật.
        if (boundedIndex >= pages.size - PRELOAD_NEAR_END_THRESHOLD) {
            preloadNextChapter()
            maybeAutoDownloadNextChapter()
        }
        // Tới trang cuối = chương hoàn thành (cùng điều kiện repo: pageIndex == totalPages-1) → dọn download cũ.
        if (boundedIndex == pages.lastIndex) {
            maybeDeleteAfterRead()
        }
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

    private suspend fun loadAdjacentChapters() {
        val prev = chapterDao.getPrevChapter(mangaId, chapterId)
        val next = chapterDao.getNextChapter(mangaId, chapterId)
        _state.update {
            it.copy(
                prevChapterId = prev?.chapterId,
                prevChapterTitle = prev?.buildTitle(),
                nextChapterId = next?.chapterId,
                nextChapterTitle = next?.buildTitle(),
            )
        }
        Timber.d("loadAdjacentChapters: prev=%s next=%s", prev?.chapterId, next?.chapterId)
    }

    private fun navigateToChapter(chapterId: String?, chapterTitle: String?) {
        if (chapterId == null) return
        // Hủy auto-advance đang chờ để không điều hướng 2 lần (vd user bấm nút trong lúc timer chạy).
        autoAdvanceJob?.cancel()
        flushProgress(null)
        _effects.tryEmit(
            ReaderUiEffect.NavigateToChapter(
                mangaId = mangaId,
                chapterId = chapterId,
                chapterTitle = chapterTitle ?: "",
            ),
        )
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

}

// Độ sáng overlay tối thiểu (0.15 = tối nhất vẫn còn nhìn thấy) và tối đa (1.0 = không tối thêm).
private const val MIN_BRIGHTNESS = 0.15f
private const val MAX_BRIGHTNESS = 1.0f

// Seamless navigation (Phase 4 PR-2a)
private const val AUTO_ADVANCE_DELAY_MS = 1500L // chờ trước khi tự sang chương (hủy được nếu lướt lại)
private const val PRELOAD_IMAGE_COUNT = 3 // số ảnh đầu chương kế warm vào Coil cache
private const val PRELOAD_NEAR_END_THRESHOLD = 2 // còn ≤2 trang cuối thì bắt đầu preload

private fun ReadingMode.next(): ReadingMode = when (this) {
    ReadingMode.VERTICAL -> ReadingMode.LTR
    ReadingMode.LTR -> ReadingMode.RTL
    ReadingMode.RTL -> ReadingMode.VERTICAL
}
