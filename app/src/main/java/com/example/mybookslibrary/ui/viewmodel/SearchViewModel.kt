package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.remote.models.MangaDexConstants
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.model.MangaTag
import com.example.mybookslibrary.domain.model.SearchFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<MangaModel> = emptyList(),
    val error: String? = null,
    val availableGenres: List<MangaTag> = emptyList(),
    val availableThemes: List<MangaTag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val selectedLanguages: Set<String> = emptySet(),
    val selectedContentRatings: Set<String> = emptySet(),
    val selectedStatuses: Set<String> = emptySet(),
    val isFilterSheetOpen: Boolean = false,
    val activeFilterCount: Int = 0,
)

// ViewModel cho SearchScreen — debounce 400ms, search lại khi query (≥2 ký tự) hoặc bộ lọc đổi.
// @Suppress TooManyFunctions: các hàm onToggle* là API rõ ràng cho từng nhóm bộ lọc, tách thêm không có lợi.
@Suppress("TooManyFunctions")
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MangaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    private val _filters = MutableStateFlow(SearchFilters())

    init {
        loadTags()
        combine(_query, _filters) { query, filters -> query to filters }
            .debounce(DEBOUNCE_MS)
            .flatMapLatest { (query, filters) ->
                // flatMapLatest huỷ search trước khi có (query, filters) mới → tránh kết quả cũ
                // ghi đè sau khi người dùng xoá query/bộ lọc. Nhánh idle xoá kết quả tại chỗ.
                if (query.length < MIN_QUERY_LENGTH && filters.isEmpty()) {
                    _uiState.update { it.copy(results = emptyList(), error = null, isLoading = false) }
                    emptyFlow()
                } else {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    repository.searchManga(query, filters)
                }
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
    }

    fun onToggleTag(tagId: String) =
        updateFilters { it.copy(includedTagIds = it.includedTagIds.toggle(tagId)) }

    fun onToggleLanguage(code: String) =
        updateFilters { it.copy(languages = it.languages.toggle(code)) }

    fun onToggleContentRating(value: String) =
        updateFilters { it.copy(contentRatings = it.contentRatings.toggle(value)) }

    fun onToggleStatus(value: String) =
        updateFilters { it.copy(statuses = it.statuses.toggle(value)) }

    fun onClearFilters() {
        _filters.value = SearchFilters()
        syncFilterState(SearchFilters())
    }

    fun onOpenFilterSheet() = _uiState.update { it.copy(isFilterSheetOpen = true) }

    fun onDismissFilterSheet() = _uiState.update { it.copy(isFilterSheetOpen = false) }

    private fun loadTags() {
        viewModelScope.launch {
            repository.getTags().onSuccess { tags ->
                _uiState.update {
                    it.copy(
                        availableGenres = tags.filter { tag -> tag.group == MangaDexConstants.TAG_GROUP_GENRE },
                        availableThemes = tags.filter { tag -> tag.group == MangaDexConstants.TAG_GROUP_THEME },
                    )
                }
            }
        }
    }

    private fun updateFilters(transform: (SearchFilters) -> SearchFilters) {
        val updated = transform(_filters.value)
        _filters.value = updated
        syncFilterState(updated)
    }

    private fun syncFilterState(filters: SearchFilters) {
        _uiState.update {
            it.copy(
                selectedTagIds = filters.includedTagIds.toSet(),
                selectedLanguages = filters.languages.toSet(),
                selectedContentRatings = filters.contentRatings.toSet(),
                selectedStatuses = filters.statuses.toSet(),
                activeFilterCount = filters.includedTagIds.size + filters.languages.size +
                    filters.contentRatings.size + filters.statuses.size,
            )
        }
    }

    private fun List<String>.toggle(value: String): List<String> =
        if (contains(value)) this - value else this + value

    companion object {
        private const val DEBOUNCE_MS = 400L
        private const val MIN_QUERY_LENGTH = 2
    }
}
