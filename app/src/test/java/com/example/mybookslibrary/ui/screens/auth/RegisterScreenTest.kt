package com.example.mybookslibrary.ui.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.ui.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI test cho [RegisterScreen] qua Robolectric + Compose (JVM, không cần Hilt):
 * truyền [AuthViewModel] thật với [AuthRepository] giả vào param viewModel.
 */
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RegisterScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val libraryRepository = mockk<LibraryRepository>(relaxed = true)

    private fun viewModel() = AuthViewModel(mockk<AuthRepository>(relaxed = true), libraryRepository)

    @Test
    fun rendersTitleAndFields() {
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = viewModel())
        }

        composeRule.onNodeWithText("Create an Account").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm Password").assertIsDisplayed()
    }

    @Test
    fun registerButton_disabledWhenEmpty() {
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = viewModel())
        }

        // Button disabled khi không nhập gì
        composeRule.onNode(hasText("Register") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun passwordMismatch_showsError() {
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = viewModel())
        }

        composeRule.onNodeWithText("Email").performTextInput("user1")
        composeRule.onNodeWithText("Password").performTextInput("Abc@123423")
        composeRule.onNodeWithText("Confirm Password").performTextInput("different")

        composeRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun passwordMatch_buttonEnabled() {
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = viewModel())
        }

        composeRule.onNodeWithText("Email").performTextInput("user1")
        composeRule.onNodeWithText("Password").performTextInput("Abc@123423")
        composeRule.onNodeWithText("Confirm Password").performTextInput("Abc@123423")

        composeRule.onNode(hasText("Register") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun togglePasswordVisibility() {
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = viewModel())
        }

        composeRule.onNodeWithContentDescription("Show password").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Show password").performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }

    @Test
    fun loginLink_exists() {
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = viewModel())
        }

        // TextButton ở cuối Column — viewport nhỏ có thể chưa scroll tới → assertExists (đã compose)
        composeRule.onNodeWithText("Do you already have an account?").assertExists()
        composeRule.onNodeWithText("Login").assertExists()
    }

    @Test
    fun errorState_showsErrorMessage() {
        val authRepository = mockk<AuthRepository>(relaxed = true)
        coEvery { authRepository.registerWithEmail(any(), any()) } returns
            Result.failure(IllegalStateException("Email already taken"))
        val vm = AuthViewModel(authRepository, libraryRepository)
        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}, viewModel = vm)
        }
        composeRule.onNodeWithText("Email").performTextInput("existing")
        composeRule.onNodeWithText("Password").performTextInput("Abc@1234")
        composeRule.onNodeWithText("Confirm Password").performTextInput("Abc@1234")
        composeRule.onNode(hasText("Register") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Email already taken").assertIsDisplayed()
    }

    @Test
    fun successState_callsOnRegisterSuccess() {
        // AuthState.Success → LaunchedEffect gọi resetState() + onRegisterSuccess()
        var successCalled = false
        val repo = mockk<AuthRepository>(relaxed = true)
        coEvery { repo.registerWithEmail(any(), any()) } coAnswers
            { Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)) }
        val vm = AuthViewModel(repo, libraryRepository)
        vm.register("newuser", "Abc@1234")

        composeRule.setContent {
            RegisterScreen(onRegisterSuccess = { successCalled = true }, onNavigateToLogin = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        assert(successCalled) { "onRegisterSuccess phải được gọi khi register thành công" }
    }
}
