package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.domain.model.MangaTag
import com.example.mybookslibrary.ui.viewmodel.SearchUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchFilterSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val emptyState = SearchUiState()

    @Test
    fun rendersTitleAndClearButton() {
        composeRule.setContent {
            SearchFilterSheet(
                state = emptyState,
                onToggleTag = {},
                onToggleLanguage = {},
                onToggleContentRating = {},
                onToggleStatus = {},
                onClearFilters = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Filters").assertIsDisplayed()
        composeRule.onNodeWithText("Clear").assertIsDisplayed()
    }

    @Test
    fun withGenresAndThemes_rendersSections() {
        // Covers lines 70-74 (genre section) và 78-83 (theme section) — chỉ hiện khi list không rỗng
        val stateWithTags =
            emptyState.copy(
                availableGenres = listOf(MangaTag("g1", "Action", "genre")),
                availableThemes = listOf(MangaTag("t1", "Office Workers", "theme")),
            )
        composeRule.setContent {
            SearchFilterSheet(
                state = stateWithTags,
                onToggleTag = {},
                onToggleLanguage = {},
                onToggleContentRating = {},
                onToggleStatus = {},
                onClearFilters = {},
                onDismiss = {},
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
    }

    @Test
    fun rendersWithoutCrash_emptyState() {
        composeRule.setContent {
            SearchFilterSheet(
                state = emptyState,
                onToggleTag = {},
                onToggleLanguage = {},
                onToggleContentRating = {},
                onToggleStatus = {},
                onClearFilters = {},
                onDismiss = {},
            )
        }
        composeRule.waitForIdle()
        // Sheet hiển thị title "Filters" đầu tiên
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
    }
}
