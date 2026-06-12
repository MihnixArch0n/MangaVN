package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.ui.util.FakeImageLoader
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class WebtoonPageItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = FakeImageLoader.install()

    @After fun tearDown() = FakeImageLoader.reset()

    @Test
    fun rendersPage_withContentDescription() {
        composeRule.setContent {
            WebtoonPageItem(
                imageUrl = "https://example.com/webtoon-0.jpg",
                index = 0,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Reader page 1").assertIsDisplayed()
    }

    @Test
    fun rendersMultipleIndexes_correctDescription() {
        composeRule.setContent {
            WebtoonPageItem(
                imageUrl = "https://example.com/webtoon-4.jpg",
                index = 4,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Reader page 5").assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() {
        FakeImageLoader.reset()
        FakeImageLoader.installFailing()
        try {
            composeRule.setContent {
                WebtoonPageItem(imageUrl = "https://x/w.jpg", index = 0)
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Tap to retry").assertIsDisplayed()
        } finally {
            FakeImageLoader.reset()
            FakeImageLoader.install()
        }
    }

    @Test
    fun loadingState_showsPageLoadingIndicator() {
        FakeImageLoader.reset()
        FakeImageLoader.installPending()
        try {
            composeRule.setContent {
                WebtoonPageItem(imageUrl = "https://x/pending.jpg", index = 2)
            }
            composeRule.onNodeWithContentDescription("Loading page 3").assertIsDisplayed()
        } finally {
            FakeImageLoader.reset()
            FakeImageLoader.install()
        }
    }
}
