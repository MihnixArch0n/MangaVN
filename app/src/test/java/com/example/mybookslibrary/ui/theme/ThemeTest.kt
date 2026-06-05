package com.example.mybookslibrary.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Kiểm tra MyBooksLibraryTheme áp đúng color scheme cho light/dark mode.
 * Đảm bảo cả hai branch của `if (darkTheme)` được thực thi.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun myBooksLibraryTheme_lightMode_appliesLightColors() {
        var primary = Color.Unspecified
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = false) {
                primary = MaterialTheme.colorScheme.primary
            }
        }
        composeRule.waitForIdle()
        assertNotEquals(Color.Unspecified, primary)
    }

    @Test
    fun myBooksLibraryTheme_darkMode_appliesDarkColors() {
        var primary = Color.Unspecified
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
            }
        }
        composeRule.waitForIdle()
        assertNotEquals(Color.Unspecified, primary)
    }

    @Test
    fun myBooksLibraryTheme_lightAndDark_haveDifferentPrimaryColors() {
        var isDark by mutableStateOf(false)
        var capturedPrimary = Color.Unspecified

        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = isDark) {
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }
        composeRule.waitForIdle()
        val lightPrimary = capturedPrimary

        isDark = true
        composeRule.waitForIdle()
        val darkPrimary = capturedPrimary

        assertNotEquals(lightPrimary, darkPrimary)
    }
}
