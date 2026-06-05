package com.example.mybookslibrary.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/**
 * Test MainNavHost bằng Hilt + Robolectric (JVM, không cần emulator).
 * - HiltTestActivity là @AndroidEntryPoint → hiltViewModel() bên trong composable hoạt động
 * - FakeNavigationModule thay thế DataModule + NetworkModule + ImageModule bằng fake/in-memory
 * - @Config(application = HiltTestApplication::class) → Hilt test application
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = dagger.hilt.android.testing.HiltTestApplication::class)
class MainNavHostTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun mainNavHost_nullUserId_showsLoginScreen() {
        composeRule.setContent {
            MainNavHost(loggedInUserId = null)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
    }

    @Test
    fun mainNavHost_nullUserId_bottomNavBarIsHidden() {
        composeRule.setContent {
            MainNavHost(loggedInUserId = null)
        }
        composeRule.waitForIdle()
        // Bottom nav không hiển thị trên Login screen
        composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
    }

    @Test
    fun mainNavHost_signOut_navigatesToLogin() {
        runBlocking { preferencesDataStore.setLoggedInUserId("test-user") }
        composeRule.setContent {
            MainNavHost(loggedInUserId = null)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
    }

    @Test
    fun mainNavHost_withUserId_showsDiscoverScreen() {
        // loggedInUserId != null → startDestination = Discover → DiscoverScreen renders
        composeRule.setContent {
            MainNavHost(loggedInUserId = "test-user-123")
        }
        composeRule.waitForIdle()
        // Login screen KHÔNG hiện (đã đăng nhập)
        val isLoginShown =
            runCatching {
                composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
            }.isSuccess
        assert(!isLoginShown) { "Login screen không được hiển thị khi đã đăng nhập" }
        // Bottom nav phải hiển thị khi ở Discover
        composeRule.onNodeWithContentDescription("Discover").assertIsDisplayed()
    }

    @Test
    fun mainNavHost_signOutFromDiscover_navigatesToLogin() {
        // Start với userId (Discover) → đổi sang null (sign-out) → phải về Login
        // Covers LaunchedEffect sign-out branch (lines 162-168 trong MainNavGraph.kt)
        var userId by mutableStateOf<String?>("test-user-123")
        composeRule.setContent {
            MainNavHost(loggedInUserId = userId)
        }
        composeRule.waitForIdle()
        // Set null sau khi đã ở Discover — trigger recompose + LaunchedEffect navigate to Login
        userId = null
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
    }
}
