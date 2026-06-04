package com.example.mybookslibrary.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * UI test cho [SettingScreenContent] qua Robolectric + Compose (JVM, không cần Hilt):
 * kiểm tra các row setting hiển thị đúng dựa trên trạng thái ViewModel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(coil3.annotation.ExperimentalCoilApi::class)
class SettingScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val prefs = mockk<UserPreferencesDataStore>(relaxed = true)
    private val libraryRepo = mockk<LibraryRepository>(relaxed = true)
    private val imageLoader = mockk<ImageLoader>(relaxed = true)

    private fun viewModel(
        quality: String = "data",
        theme: String = "system",
        language: String = "en",
    ): SettingsViewModel {
        coEvery { prefs.getReaderQuality() } returns quality
        coEvery { prefs.getThemeMode() } returns theme
        coEvery { prefs.getLanguage() } returns language
        return SettingsViewModel(
            preferencesDataStore = prefs,
            libraryRepository = libraryRepo,
            imageLoader = imageLoader,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun rendersSectionLabelsAndRows() {
        composeRule.setContent {
            SettingScreenContent(viewModel = viewModel())
        }

        // Các row đầu trong viewport hiển thị được
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Language").assertIsDisplayed()
        composeRule.onNodeWithText("Image Quality").assertIsDisplayed()
        composeRule.onNodeWithText("Clear Image Cache").assertIsDisplayed()
    }

    @Test
    fun qualityOriginal_showsCorrectLabel() {
        composeRule.setContent {
            SettingScreenContent(viewModel = viewModel(quality = "data"))
        }

        // quality="data" -> label "Original" (settings_quality_original)
        composeRule.onNodeWithText("Original").assertIsDisplayed()
    }

    @Test
    fun qualityDataSaver_showsDataSaverLabel() {
        composeRule.setContent {
            SettingScreenContent(viewModel = viewModel(quality = "data-saver"))
        }

        composeRule.onNodeWithText("Data Saver").assertIsDisplayed()
    }

    @Test
    fun languageVietnamese_showsVietnameseLabel() {
        composeRule.setContent {
            SettingScreenContent(viewModel = viewModel(language = "vi"))
        }

        composeRule.onNodeWithText("Tiếng Việt").assertIsDisplayed()
    }

    @Test
    fun screenLoadsWithoutCrash() {
        composeRule.setContent { SettingScreenContent(viewModel = viewModel()) }
        composeRule.waitForIdle()
    }

    @Test
    fun qualityToggle_rendersNewLabel() {
        composeRule.setContent { SettingScreenContent(viewModel = viewModel(quality = "data-saver")) }
        composeRule.onNodeWithText("Data Saver").assertIsDisplayed()
    }

    @Test
    fun themeLight_rendersLightLabel() {
        composeRule.setContent { SettingScreenContent(viewModel = viewModel(theme = "light")) }
        composeRule.onNodeWithText("Light").assertIsDisplayed()
    }

    @Test
    fun themeDark_rendersDarkLabel() {
        composeRule.setContent { SettingScreenContent(viewModel = viewModel(theme = "dark")) }
        composeRule.onNodeWithText("Dark").assertIsDisplayed()
    }
}
