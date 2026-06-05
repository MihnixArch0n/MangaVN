package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MangaReviewScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
