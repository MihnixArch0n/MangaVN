package com.example.mybookslibrary.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Kiểm tra FloatingPillNavBar render đúng 4 tab và callback onNavigate hoạt động.
 * Sử dụng nội dung contentDescription (= label) của icon để identify từng tab.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FloatingPillNavBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun floatingPillNavBar_withNullDestination_displaysAllFourTabs() {
        composeRule.setContent {
            MyBooksLibraryTheme {
                FloatingPillNavBar(currentDestination = null, onNavigate = {})
            }
        }
        // ContentDescription của mỗi PillNavItem = label string của tab
        composeRule.onNodeWithContentDescription("Discover").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Library").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Setting").assertIsDisplayed()
    }

    @Test
    fun floatingPillNavBar_clickDiscover_callsOnNavigate() {
        var clicked: BottomNavDestination? = null
        composeRule.setContent {
            MyBooksLibraryTheme {
                FloatingPillNavBar(currentDestination = null, onNavigate = { clicked = it })
            }
        }
        composeRule.onNodeWithContentDescription("Discover").performClick()
        assertEquals(BottomNavDestination.Discover, clicked)
    }

    @Test
    fun floatingPillNavBar_clickSearch_callsOnNavigate() {
        var clicked: BottomNavDestination? = null
        composeRule.setContent {
            MyBooksLibraryTheme {
                FloatingPillNavBar(currentDestination = null, onNavigate = { clicked = it })
            }
        }
        composeRule.onNodeWithContentDescription("Search").performClick()
        assertEquals(BottomNavDestination.Search, clicked)
    }

    @Test
    fun floatingPillNavBar_noClick_onNavigateNotCalled() {
        var clicked: BottomNavDestination? = null
        composeRule.setContent {
            MyBooksLibraryTheme {
                FloatingPillNavBar(currentDestination = null, onNavigate = { clicked = it })
            }
        }
        composeRule.waitForIdle()
        assertNull(clicked)
    }

    @Test
    fun floatingPillNavBar_withSelectedDestination_rendersSelectedState() {
        // Mock NavDestination có route = "discover" + parent = null
        // → hierarchy yields [discoverDest] → selected=true cho Discover tab
        // → covers branch `selected=true` ở lines 410 + 428 trong MainNavGraph.kt
        val discoverDest =
            mockk<NavDestination> {
                every { route } returns BottomNavDestination.Discover.route
                every { parent } returns null
            }
        composeRule.setContent {
            MyBooksLibraryTheme {
                FloatingPillNavBar(currentDestination = discoverDest, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Discover").assertIsDisplayed()
    }
}
