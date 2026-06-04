package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import com.example.mybookslibrary.ui.util.FakeImageLoader
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class MangaPageItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before fun setUp() = FakeImageLoader.install()
    @After fun tearDown() = FakeImageLoader.reset()

    @Test
    fun rendersPage_withContentDescription() {
        composeRule.setContent {
            MangaPageItem(
                imageUrl = "https://example.com/page-1.jpg",
                index = 0,
                modifier = Modifier.fillMaxSize()
            )
        }
        composeRule.waitForIdle()
        // content description = "Reader page 1" (reader_page_description %1$d)
        composeRule.onNodeWithContentDescription("Reader page 1").assertIsDisplayed()
    }

    @Test
    fun rendersPage2_correctDescription() {
        composeRule.setContent {
            MangaPageItem(
                imageUrl = "https://example.com/page-2.jpg",
                index = 1,
                modifier = Modifier.fillMaxSize()
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Reader page 2").assertIsDisplayed()
    }

    @Test
    fun rendersWithoutCrash_longPressNull() {
        composeRule.setContent {
            MangaPageItem(
                imageUrl = "https://example.com/p.jpg",
                index = 3,
                onLongPress = null
            )
        }
        composeRule.waitForIdle()
    }
}
