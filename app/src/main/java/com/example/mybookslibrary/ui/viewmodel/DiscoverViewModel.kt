package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val items: List<MangaModel> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: MangaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadDiscover()
    }

    fun loadDiscover(limit: Int = 20, offset: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getDiscoverManga(limit, offset).collect { result ->
                result.onSuccess { mangas ->
                    _uiState.value = DiscoverUiState(
                        isLoading = false,
                        items = mangas,
                        error = null
                    )
                }.onFailure { throwable ->
                    _uiState.value = DiscoverUiState(
                        isLoading = false,
                        items = emptyList(),
                        error = throwable.message ?: "Failed to load discover mangas"
                    )
                }
            }
        }
    }
}

class DiscoverViewModelFactory(
    private val repository: MangaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoverViewModel::class.java)) {
            return DiscoverViewModel(repository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}

