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
import kotlinx.coroutines.Dispatchers
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
    val isInLibrary: Boolean = false
)

@HiltViewModel
class MangaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mangaRepository: MangaRepository,
    private val libraryRepository: LibraryRepository,
    private val getChapterListWithProgressUseCase: GetChapterListWithProgressUseCase
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
        viewModelScope.launch(Dispatchers.IO) {
            mangaRepository.getMangaDetail(mangaId).onSuccess { manga ->
                _uiState.update { it.copy(mangaDetail = manga, detailError = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(detailError = e.message) }
            }
        }
    }

    private fun observeChapters() {
        if (mangaId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingChapters = true, chaptersError = null) }
            try {
                getChapterListWithProgressUseCase(mangaId).collect { chapters ->
                    _uiState.update {
                        it.copy(
                            chapters = chapters,
                            isLoadingChapters = false,
                            chaptersError = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingChapters = false, chaptersError = e.message) }
            }
        }
    }

    private fun checkLibraryStatus() {
        if (mangaId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val inLib = libraryRepository.isInLibrary(mangaId)
            _uiState.update { it.copy(isInLibrary = inLib) }
        }
    }

    fun ensureInLibrary(title: String, coverUrl: String) {
        if (_uiState.value.isInLibrary) return
        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.addToLibrary(mangaId = mangaId, title = title, coverUrl = coverUrl)
            _uiState.update { it.copy(isInLibrary = true) }
        }
    }

    fun toggleLibrary(title: String, coverUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.isInLibrary) {
                libraryRepository.removeFromLibrary(mangaId)
            } else {
                libraryRepository.addToLibrary(mangaId = mangaId, title = title, coverUrl = coverUrl)
            }
            _uiState.update { it.copy(isInLibrary = !it.isInLibrary) }
        }
    }

    fun markChapterCompleted(chapterId: String, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.markChapterCompleted(mangaId, chapterId, totalPages)
        }
    }

    fun markChapterUnread(chapterId: String, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.markChapterUnread(mangaId, chapterId, totalPages)
        }
    }
}
