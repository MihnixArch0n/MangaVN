package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.remote.models.FirestoreReview
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.ReviewRepository
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.usecase.GetChapterListWithProgressUseCase
import com.example.mybookslibrary.ui.navigation.MangaDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt

data class MangaDetailUiState(
    val mangaDetail: MangaModel? = null,
    val detailError: String? = null,
    val chapters: List<ChapterWithProgressModel> = emptyList(),
    val availableLanguages: List<String> = emptyList(),
    val selectedLanguage: String = "",
    val isLoadingChapters: Boolean = false,
    val chaptersError: String? = null,
    val isInLibrary: Boolean = false,
    val isFavorite: Boolean = false,
    val firstChapterPages: List<String> = emptyList(),
    val isLoadingFirstChapterPages: Boolean = false,
    val firstChapterPagesError: String? = null,
    val lastReadChapterId: String? = null,
    /** Tối đa 3 review mới nhất cho section Customer Reviews. */
    val topReviews: List<FirestoreReview> = emptyList(),
    /** Điểm TB (1 chữ số thập phân) trên tập review đã fetch; null khi chưa có review. */
    val reviewAverage: Double? = null,
    val reviewCount: Int = 0,
)

@HiltViewModel
class MangaDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val mangaRepository: MangaRepository,
        private val libraryRepository: LibraryRepository,
        private val getChapterListWithProgressUseCase: GetChapterListWithProgressUseCase,
        private val offlineDownloadManager: OfflineDownloadManager,
        private val userPreferencesDataStore: UserPreferencesDataStore,
        private val reviewRepository: ReviewRepository,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val mangaId: String = savedStateHandle.toRoute<MangaDetail>().mangaId

        private val _uiState = MutableStateFlow(MangaDetailUiState())
        val uiState: StateFlow<MangaDetailUiState> = _uiState.asStateFlow()

        init {
            loadMangaDetail()
            observeChapters()
            checkLibraryStatus()
            loadTopReviews()
        }

        /** Top 3 review mới nhất + điểm TB cho section Customer Reviews — lỗi thì giữ rỗng (im lặng). */
        private fun loadTopReviews() {
            if (mangaId.isBlank()) return
            launchSafe {
                val reviews = reviewRepository.getReviews(mangaId)
                val average =
                    if (reviews.isEmpty()) {
                        null
                    } else {
                        (reviews.sumOf { it.rating }.toDouble() / reviews.size * AVERAGE_ROUND_FACTOR)
                            .roundToInt() / AVERAGE_ROUND_FACTOR
                    }
                _uiState.update {
                    it.copy(
                        topReviews = reviews.take(TOP_REVIEWS_COUNT),
                        reviewAverage = average,
                        reviewCount = reviews.size,
                    )
                }
            }
        }

        // Chạy tác vụ Room/IO trong viewModelScope có bắt lỗi, tránh crash app do exception không xử lý.
        private fun launchSafe(block: suspend () -> Unit) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    block()
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    Timber.e(e, "MangaDetailViewModel: task failed")
                }
            }
        }

        private fun loadMangaDetail() {
            if (mangaId.isBlank()) return
            viewModelScope.launch(ioDispatcher) {
                mangaRepository
                    .getMangaDetail(mangaId)
                    .onSuccess { manga ->
                        _uiState.update { it.copy(mangaDetail = manga, detailError = null) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(detailError = e.message) }
                    }
            }
        }

        private fun observeChapters() {
            if (mangaId.isBlank()) return
            viewModelScope.launch(ioDispatcher) {
                _uiState.update { it.copy(isLoadingChapters = true, chaptersError = null) }
                try {
                    getChapterListWithProgressUseCase(mangaId).collect { result ->
                        val isFirstLoad = _uiState.value.chapters.isEmpty() && result.chapters.isNotEmpty()
                        _uiState.update {
                            it.copy(
                                chapters = result.chapters,
                                availableLanguages = result.availableLanguages,
                                selectedLanguage = result.selectedLanguage,
                                isLoadingChapters = false,
                                chaptersError = null,
                            )
                        }
                        if (isFirstLoad) {
                            loadFirstChapterPages(result.chapters.first().chapterId)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoadingChapters = false, chaptersError = e.message) }
                }
            }
        }

        private fun loadFirstChapterPages(chapterId: String) {
            viewModelScope.launch(ioDispatcher) {
                _uiState.update { it.copy(isLoadingFirstChapterPages = true, firstChapterPagesError = null) }
                mangaRepository
                    .getChapterPages(chapterId)
                    .onSuccess { pages ->
                        _uiState.update {
                            it.copy(
                                firstChapterPages = pages.take(5),
                                isLoadingFirstChapterPages = false,
                                firstChapterPagesError = null,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoadingFirstChapterPages = false,
                                firstChapterPagesError = e.message,
                            )
                        }
                    }
            }
        }

        private fun checkLibraryStatus() {
            if (mangaId.isBlank()) return
            launchSafe {
                val item = libraryRepository.getLibraryItem(mangaId)
                _uiState.update {
                    it.copy(
                        isInLibrary = item != null,
                        isFavorite = item?.is_favorite == true,
                        lastReadChapterId = item?.last_read_chapter_id,
                    )
                }
            }
        }

        fun selectLanguage(language: String) {
            launchSafe {
                userPreferencesDataStore.setPreferredChapterLanguage(language)
            }
        }

        fun ensureInLibrary(
            title: String,
            coverUrl: String,
        ) {
            if (_uiState.value.isInLibrary) return
            launchSafe {
                libraryRepository.addToLibrary(mangaId = mangaId, title = title, coverUrl = coverUrl)
                _uiState.update { it.copy(isInLibrary = true) }
            }
        }

        fun toggleLibrary(
            title: String,
            coverUrl: String,
        ) {
            launchSafe {
                if (_uiState.value.isInLibrary) {
                    // Xóa khỏi thư viện đồng nghĩa mất luôn cờ yêu thích (row bị xóa)
                    libraryRepository.removeFromLibrary(mangaId)
                    _uiState.update { it.copy(isInLibrary = false, isFavorite = false) }
                } else {
                    libraryRepository.addToLibrary(mangaId = mangaId, title = title, coverUrl = coverUrl)
                    _uiState.update { it.copy(isInLibrary = true) }
                }
            }
        }

        fun toggleFavorite(
            title: String,
            coverUrl: String,
        ) {
            launchSafe {
                val newFavorite = !_uiState.value.isFavorite
                libraryRepository.setFavorite(
                    mangaId = mangaId,
                    title = title,
                    coverUrl = coverUrl,
                    isFavorite = newFavorite,
                )
                _uiState.update {
                    it.copy(
                        isFavorite = newFavorite,
                        // Yêu thích manga chưa có trong thư viện → repository tự thêm vào
                        isInLibrary = it.isInLibrary || newFavorite,
                    )
                }
            }
        }

        fun markChapterCompleted(
            chapterId: String,
            totalPages: Int,
        ) {
            launchSafe {
                libraryRepository.markChapterCompleted(mangaId, chapterId, totalPages)
            }
        }

        fun markChapterUnread(
            chapterId: String,
            totalPages: Int,
        ) {
            launchSafe {
                libraryRepository.markChapterUnread(mangaId, chapterId, totalPages)
            }
        }

        fun startChapterDownload(chapterId: String) {
            if (mangaId.isBlank() || chapterId.isBlank()) return
            launchSafe {
                val chapter = uiState.value.chapters.firstOrNull { it.chapterId == chapterId }
                offlineDownloadManager.enqueueDownload(
                    mangaId = mangaId,
                    chapterId = chapterId,
                    mangaTitle = uiState.value.mangaDetail?.title,
                    chapterLabel = chapter?.buildDownloadLabel(),
                )
            }
        }

        fun cancelChapterDownload(chapterId: String) {
            if (mangaId.isBlank() || chapterId.isBlank()) return
            launchSafe {
                offlineDownloadManager.cancelDownload(mangaId, chapterId)
            }
        }

        fun deleteChapterDownload(chapterId: String) {
            if (mangaId.isBlank() || chapterId.isBlank()) return
            launchSafe {
                offlineDownloadManager.deleteDownload(mangaId, chapterId)
            }
        }

        companion object {
            private const val TOP_REVIEWS_COUNT = 3
            private const val AVERAGE_ROUND_FACTOR = 10.0
        }
    }

private fun ChapterWithProgressModel.buildDownloadLabel(): String =
    buildString {
        volume?.takeIf { it.isNotBlank() }?.let { append("Vol. $it ") }
        chapterNumber?.takeIf { it.isNotBlank() }?.let { append("Ch. $it") }
        title?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
        if (isEmpty()) append("Oneshot")
    }
