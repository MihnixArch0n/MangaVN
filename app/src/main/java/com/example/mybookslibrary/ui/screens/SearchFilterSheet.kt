package com.example.mybookslibrary.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.remote.models.MangaDexConstants
import com.example.mybookslibrary.ui.screens.components.AppButton
import com.example.mybookslibrary.ui.screens.components.AppButtonStyle
import com.example.mybookslibrary.ui.screens.components.AppFilterChip
import com.example.mybookslibrary.ui.screens.components.ErrorMessageBox
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.SearchUiState

/**
 * Bottom sheet chọn bộ lọc cho màn Search: thể loại, chủ đề, ngôn ngữ, mức nội dung, trạng thái.
 * State + callbacks do [SearchViewModel] cấp; sheet chỉ hiển thị và phát sự kiện toggle.
 */
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    state: SearchUiState,
    onToggleTag: (String) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onToggleContentRating: (String) -> Unit,
    onToggleStatus: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPaddingCompact)
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = appString(R.string.filter_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = appString(R.string.filter_clear),
                    onClick = onClearFilters,
                    style = AppButtonStyle.Text,
                )
            }

            if (state.tagsError) {
                Spacer(Modifier.height(Dimens.SpacingSm))
                ErrorMessageBox(appString(R.string.filter_tags_error))
            }
            if (state.availableGenres.isNotEmpty()) {
                FilterChipGroup(
                    titleRes = R.string.filter_section_genre,
                    options = state.availableGenres.map { it.id to it.name },
                    selected = state.selectedTagIds,
                    onToggle = onToggleTag,
                )
            }
            if (state.availableThemes.isNotEmpty()) {
                FilterChipGroup(
                    titleRes = R.string.filter_section_theme,
                    options = state.availableThemes.map { it.id to it.name },
                    selected = state.selectedTagIds,
                    onToggle = onToggleTag,
                )
            }
            FilterChipGroup(
                titleRes = R.string.filter_section_language,
                options =
                    listOf(
                        MangaDexConstants.LANG_EN to appString(R.string.filter_lang_en),
                        MangaDexConstants.LANG_VI to appString(R.string.filter_lang_vi),
                        MangaDexConstants.LANG_JA to appString(R.string.filter_lang_ja),
                    ),
                selected = state.selectedLanguages,
                onToggle = onToggleLanguage,
            )
            FilterChipGroup(
                titleRes = R.string.filter_section_rating,
                options =
                    listOf(
                        MangaDexConstants.RATING_SAFE to appString(R.string.filter_rating_safe),
                        MangaDexConstants.RATING_SUGGESTIVE to appString(R.string.filter_rating_suggestive),
                        MangaDexConstants.RATING_EROTICA to appString(R.string.filter_rating_erotica),
                    ),
                selected = state.selectedContentRatings,
                onToggle = onToggleContentRating,
            )
            FilterChipGroup(
                titleRes = R.string.filter_section_status,
                options =
                    listOf(
                        MangaDexConstants.STATUS_ONGOING to appString(R.string.filter_status_ongoing),
                        MangaDexConstants.STATUS_COMPLETED to appString(R.string.filter_status_completed),
                        MangaDexConstants.STATUS_HIATUS to appString(R.string.filter_status_hiatus),
                        MangaDexConstants.STATUS_CANCELLED to appString(R.string.filter_status_cancelled),
                    ),
                selected = state.selectedStatuses,
                onToggle = onToggleStatus,
            )

            Spacer(Modifier.height(Dimens.SpacingLg))
            AppButton(
                text = appString(R.string.filter_apply),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.SpacingLg))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    @StringRes titleRes: Int,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Spacer(Modifier.height(Dimens.SpacingLg))
    Text(
        text = appString(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(Dimens.SpacingSm))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
        options.forEach { (value, label) ->
            AppFilterChip(
                label = label,
                selected = value in selected,
                onClick = { onToggle(value) },
            )
        }
    }
}
