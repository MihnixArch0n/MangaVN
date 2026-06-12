package com.example.mybookslibrary.ui.screens.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.ui.viewmodel.AuthViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression test cho [LoginScreen] (Roborazzi + Robolectric, chạy JVM không cần emulator).
 *
 * Golden lưu ở src/test/screenshots (tracked) để CI verify. Tạo/cập nhật golden bằng
 * `./gradlew recordRoborazziDebug`; CI so khớp bằng `verifyRoborazziDebug`.
 * Lưu ý: golden phụ thuộc OS render — nên record + verify trên CÙNG OS (ubuntu CI).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun loginScreen_default() {
        composeRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                onNavigateToRegister = {},
                viewModel = AuthViewModel(authRepository, mockk(relaxed = true)),
            )
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/login_screen_default.png")
    }
}
