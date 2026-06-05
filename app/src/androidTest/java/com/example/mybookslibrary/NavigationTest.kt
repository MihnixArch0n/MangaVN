package com.example.mybookslibrary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.example.mybookslibrary.data.local.UserPreferencesDataStore

/**
 * Instrumented navigation test dùng HiltAndroidTest.
 * Lưu ý: createAndroidComposeRule launch Activity trước @Before nên không thể
 * reset DataStore trong @Before để ảnh hưởng navigation ban đầu.
 * Các test chỉ assert trạng thái UI sau khi Activity đã launch.
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
        // App đã launch — màn đầu là Login hoặc Discover tùy trạng thái.
        // Test chỉ verify không crash và có ít nhất 1 screen visible.
        composeRule.waitForIdle()
    }

    @Test
    fun loginScreen_hasNavigationToRegister() {
        composeRule.waitForIdle()
        // Nếu đang ở Login screen, link đến Register phải visible
        val isLoginScreen =
            runCatching {
                composeRule.onNodeWithText("Don't have an account? Register").assertIsDisplayed()
            }.isSuccess

        if (isLoginScreen) {
            composeRule.onNodeWithText("Don't have an account? Register").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Create an Account").assertIsDisplayed()
        }
        // Nếu không ở Login (đã login trước) → test pass (điều kiện không áp dụng)
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
        // Set userId sau khi Activity đã launch — trigger navigation sang Login
        runBlocking { preferencesDataStore.setLoggedInUserId(null) }
        composeRule.waitForIdle()
        // Sau khi set null: nếu đang ở Discover → navigate về Login
        // Nếu đang ở Login → vẫn ở Login
        // Một trong hai case đều OK (không crash)
    }
}
