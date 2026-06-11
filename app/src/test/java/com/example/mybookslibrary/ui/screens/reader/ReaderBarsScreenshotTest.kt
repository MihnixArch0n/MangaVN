package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot test: capture chỉ active khi chạy record/verifyRoborazziDebug (CI).
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderBarsScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun readerBars_visible_light() {
        capture(darkTheme = false)
    }

    @Test
    fun readerBars_visible_dark() {
        capture(darkTheme = true)
    }

    private fun capture(darkTheme: Boolean) {
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize()) {
                    ReaderTopBar(
                        chapterTitle = "Chapter 5: The Journey",
                        isVisible = true,
                        onBackClick = {},
                    )
                    ReaderBottomBar(
                        isVisible = true,
                        currentPage = 4,
                        totalPages = 20,
                        currentReadingMode = ReadingMode.LTR,
                        onToggleReadingMode = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }
}
