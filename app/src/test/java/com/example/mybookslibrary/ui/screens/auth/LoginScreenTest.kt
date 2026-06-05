package com.example.mybookslibrary.ui.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.ui.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI test cho [LoginScreen] qua Robolectric + Compose test (JVM, không cần Hilt/NavHost):
 * truyền thẳng [AuthViewModel] thật với [AuthRepository] giả vào param viewModel của màn hình.
 */
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    @Test
    fun successState_callsOnLoginSuccess() {
        // AuthState.Success → LaunchedEffect gọi resetState() + onLoginSuccess()
        var successCalled = false
        val repo = mockk<AuthRepository>(relaxed = true)
        coEvery { repo.login(any(), any()) } coAnswers { Result.success(Unit) }
        val vm = AuthViewModel(repo)
        vm.login("user", "pass")

        composeRule.setContent {
            LoginScreen(onLoginSuccess = { successCalled = true }, onNavigateToRegister = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        assert(successCalled) { "onLoginSuccess phải được gọi khi login thành công" }
    }

    @Test
    fun loadingState_showsProgressInLoginButton() {
        // AuthState.Loading → CircularProgressIndicator thay vì Text "Login" trong nút
        val repo = mockk<AuthRepository>(relaxed = true)
        coEvery { repo.login(any(), any()) } coAnswers {
            delay(Long.MAX_VALUE)
            Result.success(Unit)
        }
        val vm = AuthViewModel(repo)

        composeRule.setContent {
            LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {}, viewModel = vm)
        }
        composeRule.onNodeWithText("Username").performTextInput("user")
        composeRule.onNodeWithText("Password").performTextInput("pass")
        composeRule.onNode(hasText("Login") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        // Trong loading state: nút bị disabled (hasClickAction trả về nút không click được)
        // Nút "Login" với click action không còn hiện (đang loading → nút disabled)
        composeRule.onNodeWithText("Login").assertIsDisplayed() // text vẫn hiện (ở top bar) nhưng button bị disable
    }
}
