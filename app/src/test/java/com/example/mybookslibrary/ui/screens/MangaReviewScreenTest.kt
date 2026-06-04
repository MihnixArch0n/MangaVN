package com.example.mybookslibrary.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MangaReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithBackButton() {
        composeRule.setContent {
            MangaReviewScreen(onBackClick = {})
        }
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun rendersWithoutCrash_emptyReviews() {
        composeRule.setContent {
            MangaReviewScreen(onBackClick = {})
        }
        composeRule.waitForIdle()
    }
}
