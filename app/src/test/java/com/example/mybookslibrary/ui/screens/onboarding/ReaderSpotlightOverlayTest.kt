package com.example.mybookslibrary.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderSpotlightOverlayTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hiddenOverlay_isRemovedImmediately() {
        val visible = mutableStateOf(true)
        composeRule.setContent {
            ReaderSpotlightOverlay(visible = visible.value, onDismiss = {})
        }

        composeRule
            .onNodeWithContentDescription("Tap left/right to flip pages, center to open menu")
            .assertIsDisplayed()

        composeRule.runOnIdle { visible.value = false }

        composeRule
            .onNodeWithContentDescription("Tap left/right to flip pages, center to open menu")
            .assertDoesNotExist()
    }
}
