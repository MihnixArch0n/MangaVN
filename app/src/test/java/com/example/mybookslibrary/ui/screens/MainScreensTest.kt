package com.example.mybookslibrary.ui.screens

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.SearchViewModel
import coil3.ImageLoader
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Test cho các wrapper screen trong MainScreens.kt (SearchScreen, SettingScreen).
 * Truyền ViewModel trực tiếp vì wrapper có default hiltViewModel().
 */
@SuppressLint("ViewModelConstructorInComposable")
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class MainScreensTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    @Test
    fun searchScreen_rendersWithoutCrash() {
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))
        composeRule.setContent {
            SearchScreen(viewModel = SearchViewModel(repo))
        }
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun settingScreen_rendersWithoutCrash() {
        val prefs = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefs.getReaderQuality() } returns "data"
        coEvery { prefs.getThemeMode() } returns "system"
        coEvery { prefs.getLanguage() } returns "en"
        composeRule.setContent {
            SettingScreen(
                viewModel =
                    SettingsViewModel(
                        preferencesDataStore = prefs,
                        libraryRepository = mockk<LibraryRepository>(relaxed = true),
                        imageLoader = mockk<ImageLoader>(relaxed = true),
                        ioDispatcher = UnconfinedTestDispatcher(),
                    ),
            )
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
