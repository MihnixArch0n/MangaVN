package com.example.mybookslibrary.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.ui.viewmodel.SearchUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchFilterSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

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
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("Filters").assertIsDisplayed()
        composeRule.onNodeWithText("Clear").assertIsDisplayed()
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
                onDismiss = {}
            )
        }
        composeRule.waitForIdle()
        // Sheet hiển thị title "Filters" đầu tiên
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
    }
}
