package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.usecase.GetChapterListWithProgressUseCase
import com.example.mybookslibrary.ui.navigation.MangaDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import com.example.mybookslibrary.di.IoDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MangaDetailUiState(
    val mangaDetail: MangaModel? = null,
    val detailError: String? = null,
    val chapters: List<ChapterWithProgressModel> = emptyList(),
    val isLoadingChapters: Boolean = false,
    val chaptersError: String? = null,
    val isInLibrary: Boolean = false,
    val firstChapterPages: List<String> = emptyList(),
    val isLoadingFirstChapterPages: Boolean = false,
    val firstChapterPagesError: String? = null
)

@HiltViewModel
class MangaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mangaRepository: MangaRepository,
    private val libraryRepository: LibraryRepository,
    private val getChapterListWithProgressUseCase: GetChapterListWithProgressUseCase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val mangaId: String = savedStateHandle.get<String>(
        MangaDetailDestination.mangaIdArgumentName
    ).orEmpty()

    private val _uiState = MutableStateFlow(MangaDetailUiState())
    val uiState: StateFlow<MangaDetailUiState> = _uiState.asStateFlow()

    init {
        loadMangaDetail()
        observeChapters()
        checkLibraryStatus()
    }

    private fun loadMangaDetail() {
        if (mangaId.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            mangaRepository.getMangaDetail(mangaId).onSuccess { manga ->
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
                getChapterListWithProgressUseCase(mangaId).collect { chapters ->
                    val isFirstLoad = _uiState.value.chapters.isEmpty() && chapters.isNotEmpty()
                    _uiState.update {
                        it.copy(
                            chapters = chapters,
                            isLoadingChapters = false,
                            chaptersError = null
                        )
                    }
                    if (isFirstLoad) {
                        loadFirstChapterPages(chapters.first().chapterId)
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
            mangaRepository.getChapterPages(chapterId).onSuccess { pages ->
                _uiState.update { 
                    it.copy(
                        firstChapterPages = pages.take(5), 
                        isLoadingFirstChapterPages = false,
                        firstChapterPagesError = null
                    ) 
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoadingFirstChapterPages = false, 
                        firstChapterPagesError = e.message
                    ) 
                }
            }
        }
    }

    private fun checkLibraryStatus() {
        if (mangaId.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val inLib = libraryRepository.isInLibrary(mangaId)
            _uiState.update { it.copy(isInLibrary = inLib) }
        }
    }

    fun ensureInLibrary(title: String, coverUrl: String) {
        if (_uiState.value.isInLibrary) return
        viewModelScope.launch(ioDispatcher) {
            libraryRepository.addToLibrary(mangaId = mangaId, title = title, coverUrl = coverUrl)
            _uiState.update { it.copy(isInLibrary = true) }
        }
    }

    fun toggleLibrary(title: String, coverUrl: String) {
        viewModelScope.launch(ioDispatcher) {
            if (_uiState.value.isInLibrary) {
                libraryRepository.removeFromLibrary(mangaId)
            } else {
                libraryRepository.addToLibrary(mangaId = mangaId, title = title, coverUrl = coverUrl)
            }
            _uiState.update { it.copy(isInLibrary = !it.isInLibrary) }
        }
    }

    fun markChapterCompleted(chapterId: String, totalPages: Int) {
        viewModelScope.launch(ioDispatcher) {
            libraryRepository.markChapterCompleted(mangaId, chapterId, totalPages)
        }
    }

    fun markChapterUnread(chapterId: String, totalPages: Int) {
        viewModelScope.launch(ioDispatcher) {
            libraryRepository.markChapterUnread(mangaId, chapterId, totalPages)
        }
    }
}
