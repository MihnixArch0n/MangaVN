package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<MangaModel> = emptyList(),
    val error: String? = null
)

// ViewModel cho SearchScreen — debounce 400ms, gọi API tìm kiếm khi query ≥ 2 ký tự
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MangaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        _query
            .debounce(400)
            .filter { it.length >= 2 }
            .flatMapLatest { q ->
                _uiState.update { it.copy(isLoading = true, error = null) }
                repository.searchManga(q)
            }
            .onEach { result ->
                result.onSuccess { items ->
                    _uiState.update { it.copy(isLoading = false, results = items, error = null) }
                }.onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _query.value = query
        _uiState.update { it.copy(query = query) }
        if (query.length < 2) {
            _uiState.update { it.copy(results = emptyList(), error = null, isLoading = false) }
        }
    }
}
