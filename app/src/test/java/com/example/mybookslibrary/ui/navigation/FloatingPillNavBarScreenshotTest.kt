package com.example.mybookslibrary.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
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
class FloatingPillNavBarScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun floatingPillNavBar_light() {
        capture(darkTheme = false)
    }

    @Test
    fun floatingPillNavBar_dark() {
        capture(darkTheme = true)
    }

    private fun capture(darkTheme: Boolean) {
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = darkTheme) {
                FloatingPillNavBar(currentDestination = null, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }
}
