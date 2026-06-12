package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Filter
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.navigation.LocalBottomNavPadding
import com.example.mybookslibrary.ui.navigation.LucideSearchIcon
import com.example.mybookslibrary.ui.screens.components.EmptyState
import com.example.mybookslibrary.ui.screens.components.ErrorState
import com.example.mybookslibrary.ui.screens.components.SearchSkeletonLoading
import com.example.mybookslibrary.ui.screens.components.MangaCoverCard
import com.example.mybookslibrary.ui.screens.components.StyledBadge
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.SearchViewModel

@Suppress("unused", "LongMethod")
@Composable
fun SearchScreenContent(
    modifier: Modifier = Modifier,
    onMangaClick: (MangaModel) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomNavPadding = LocalBottomNavPadding.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            Column(Modifier.padding(horizontal = Dimens.ScreenPaddingCompact)) {
                Spacer(Modifier.height(Dimens.SpacingXl))
                Text(
                    appString(R.string.search_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.SpacingLg + Dimens.SpacingXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = {
                            Text(
                                appString(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                LucideSearchIcon,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    )
                    Spacer(Modifier.width(Dimens.SpacingSm))
                    Box {
                        IconButton(onClick = viewModel::onOpenFilterSheet) {
                            Icon(
                                Lucide.Filter,
                                contentDescription = appString(R.string.cd_filter),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (uiState.activeFilterCount > 0) {
                            StyledBadge(
                                count = uiState.activeFilterCount,
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingSm))
            }

            when {
                uiState.isLoading -> {
                    SearchSkeletonLoading(Modifier.fillMaxSize())
                }
                uiState.error != null -> {
                    ErrorState(
                        message = appString(R.string.search_error_title),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.query.length < 2 && uiState.activeFilterCount == 0 -> {
                    EmptyState(
                        title = appString(R.string.search_prompt_title),
                        subtitle = appString(R.string.search_prompt_subtitle),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.results.isEmpty() -> {
                    EmptyState(
                        title =
                            if (uiState.query.isBlank()) {
                                appString(R.string.search_no_results_filter)
                            } else {
                                appString(R.string.search_no_results, uiState.query)
                            },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding =
                            PaddingValues(
                                start = Dimens.ScreenPaddingCompact,
                                end = Dimens.ScreenPaddingCompact,
                                top = Dimens.SpacingSm,
                                bottom = bottomNavPadding + Dimens.SpacingLg,
                            ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
                    ) {
                        items(uiState.results, key = { it.id }) { manga ->
                            SearchResultItem(manga) { onMangaClick(manga) }
                        }
                    }
                }
            }

            if (uiState.isFilterSheetOpen) {
                SearchFilterSheet(
                    state = uiState,
                    onToggleTag = viewModel::onToggleTag,
                    onToggleLanguage = viewModel::onToggleLanguage,
                    onToggleContentRating = viewModel::onToggleContentRating,
                    onToggleStatus = viewModel::onToggleStatus,
                    onClearFilters = viewModel::onClearFilters,
                    onDismiss = viewModel::onDismissFilterSheet,
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(manga: MangaModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.BorderThin),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCoverCard(
                coverUrl = manga.coverArt,
                contentDescription = manga.title,
                width = Dimens.IconXxl,
            )
            Spacer(Modifier.width(Dimens.SpacingLg))
            Column(Modifier.weight(1f)) {
                Text(
                    manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (manga.tags.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.SpacingXs))
                    Text(
                        manga.tags.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
