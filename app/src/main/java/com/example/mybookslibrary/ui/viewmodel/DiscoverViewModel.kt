package com.example.mybookslibrary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val items: List<MangaModel> = emptyList(),
    val error: String? = null
)

// ViewModel cho DiscoverScreen — tải danh sách manga từ MangaDex API
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    application: Application,
    private val repository: MangaRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadDiscover()
    }

    fun loadDiscover(limit: Int = 20, offset: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getDiscoverManga(limit, offset).collect { result ->
                result.onSuccess { mangas ->
                    _uiState.update { it.copy(isLoading = false, items = mangas, error = null) }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: getApplication<Application>().getString(R.string.error_load_discover)
                        )
                    }
                }
            }
        }
    }
}
