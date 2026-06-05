package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.mybookslibrary.domain.model.ReadingMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderBarsTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- ReaderTopBar ----

    @Test
    fun topBar_visible_showsTitleAndBack() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderTopBar(
                    chapterTitle = "Chapter 5",
                    isVisible = true,
                    onBackClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Chapter 5").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun topBar_notVisible_hiddenFromSemantics() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderTopBar(chapterTitle = "Ch", isVisible = false, onBackClick = {})
            }
        }

        // isVisible=false → AnimatedVisibility não compose o conteúdo
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun topBar_backClick_fires() {
        var clicked = false
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderTopBar(chapterTitle = "Ch", isVisible = true, onBackClick = { clicked = true })
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(clicked)
    }

    // ---- ReaderBottomBar ----

    @Test
    fun bottomBar_visible_showsPageCountAndToggle() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderBottomBar(
                    isVisible = true,
                    currentPage = 4,
                    totalPages = 20,
                    currentReadingMode = ReadingMode.LTR,
                    onToggleReadingMode = {},
                )
            }
        }

        // page 4 (0-based) → display "5 / 20"
        composeRule.onNodeWithText("5 / 20").assertIsDisplayed()
        composeRule.onNodeWithText("Pages").assertIsDisplayed()
    }

    @Test
    fun bottomBar_notVisible_hiddenFromSemantics() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderBottomBar(
                    isVisible = false,
                    currentPage = 0,
                    totalPages = 10,
                    currentReadingMode = ReadingMode.LTR,
                    onToggleReadingMode = {},
                )
            }
        }

        composeRule.onNodeWithText("Pages").assertDoesNotExist()
    }

    @Test
    fun bottomBar_toggleClick_fires() {
        var toggled = false
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderBottomBar(
                    isVisible = true,
                    currentPage = 0,
                    totalPages = 5,
                    currentReadingMode = ReadingMode.VERTICAL,
                    onToggleReadingMode = { toggled = true },
                )
            }
        }

        // content description = "Switch reading mode to Horizontal (LTR)" (VERTICAL → next = LTR)
        composeRule
            .onNodeWithContentDescription("Switch reading mode to Horizontal (LTR)")
            .performClick()
        assertTrue(toggled)
    }

    @Test
    fun bottomBar_pageClamp_minAndMax() {
        // page=-1 → clamp → display "1 / 5"
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderBottomBar(
                    isVisible = true,
                    currentPage = -1,
                    totalPages = 5,
                    currentReadingMode = ReadingMode.LTR,
                    onToggleReadingMode = {},
                )
            }
        }
        composeRule.onNodeWithText("1 / 5").assertIsDisplayed()
    }

    @Test
    fun bottomBar_rtlMode_showsPageCount() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderBottomBar(
                    isVisible = true,
                    currentPage = 0,
                    totalPages = 3,
                    currentReadingMode = ReadingMode.RTL,
                    onToggleReadingMode = {},
                )
            }
        }
        composeRule.onNodeWithText("1 / 3").assertIsDisplayed()
    }

    @Test
    fun bottomBar_verticalMode_showsPageCount() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ReaderBottomBar(
                    isVisible = true,
                    currentPage = 1,
                    totalPages = 5,
                    currentReadingMode = ReadingMode.VERTICAL,
                    onToggleReadingMode = {},
                )
            }
        }
        composeRule.onNodeWithText("2 / 5").assertIsDisplayed()
    }
}
