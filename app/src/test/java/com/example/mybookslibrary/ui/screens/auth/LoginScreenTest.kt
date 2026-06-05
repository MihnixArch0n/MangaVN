package com.example.mybookslibrary.ui.screens.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.ui.viewmodel.AuthViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * UI test cho [LoginScreen] qua Robolectric + Compose test (JVM, không cần Hilt/NavHost):
 * truyền thẳng [AuthViewModel] thật với [AuthRepository] giả vào param viewModel của màn hình.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    private fun viewModel() = AuthViewModel(authRepository)

    @Test
    fun rendersWelcomeTitleAndInputs() {
        composeRule.setContent {
            LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {}, viewModel = viewModel())
        }

        composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
        composeRule.onNodeWithText("Username").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test
    fun emptyInput_whenLoginTapped_showsValidationError() {
        composeRule.setContent {
            LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {}, viewModel = viewModel())
        }

        // Phân biệt nút "Login" với tiêu đề "Login" trên TopAppBar bằng hasClickAction.
        composeRule.onNode(hasText("Login") and hasClickAction()).performClick()

        composeRule.onNodeWithText("Username and password cannot be empty").assertIsDisplayed()
    }

    @Test
    fun tapPasswordToggle_switchesVisibilityIcon() {
        composeRule.setContent {
            LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {}, viewModel = viewModel())
        }

        // Mặc định ẩn mật khẩu → icon "Show password"; tap để chuyển sang "Hide password".
        composeRule.onNodeWithContentDescription("Show password").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show password").performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }
}
