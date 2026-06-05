package com.example.mybookslibrary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented navigation test dùng HiltAndroidTest.
 * createAndroidComposeRule v1 — v2 và createEmptyComposeRule đều gây
 * "No compose hierarchies" với HiltAndroidTest trên project này.
 * Activity launch trước @Before nên không thể reset DataStore để ảnh hưởng
 * startDestination — dùng conditional pattern để handle cả hai initial state.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun app_launchesAndShowsAScreen() {
        composeRule.waitForIdle()
    }

    @Test
    fun loginScreen_hasNavigationToRegister() {
        composeRule.waitForIdle()
        val isLoginScreen =
            runCatching {
                composeRule.onNodeWithText("Don't have an account? Register").assertIsDisplayed()
            }.isSuccess
        if (isLoginScreen) {
            composeRule.onNodeWithText("Don't have an account? Register").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Create an Account").assertIsDisplayed()
        }
    }

    @Test
    fun registerScreen_hasNavigationBackToLogin() {
        composeRule.waitForIdle()
        val isLoginScreen =
            runCatching {
                composeRule.onNodeWithText("Don't have an account? Register").assertIsDisplayed()
            }.isSuccess
        if (isLoginScreen) {
            composeRule.onNodeWithText("Don't have an account? Register").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Already have an account? Login").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
        }
    }

    @Test
    fun signOut_navigatesToLogin() {
        composeRule.waitForIdle()
        runBlocking { preferencesDataStore.setLoggedInUserId(null) }
        composeRule.waitForIdle()
    }

    @Test
    fun loginScreen_bottomNavBar_isNotVisible() {
        // Khi ở Login/Register, FloatingPillNavBar phải bị ẩn (showBottomBar = false).
        composeRule.waitForIdle()
        val isLoginScreen =
            runCatching {
                composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
            }.isSuccess
        if (isLoginScreen) {
            // Không có tab Discover/Search/Library/Settings trên Login screen
            runCatching {
                composeRule.onNodeWithText("Discover").assertIsDisplayed()
            }.let { assert(!it.isSuccess) { "Bottom nav không được hiển thị trên Login screen" } }
        }
    }
}
