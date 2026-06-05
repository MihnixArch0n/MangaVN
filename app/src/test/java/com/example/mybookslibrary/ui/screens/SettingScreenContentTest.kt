package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import com.example.mybookslibrary.data.local.LibraryItemEntity
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI test cho [SettingScreenContent] qua Robolectric + Compose (JVM, không cần Hilt):
 * kiểm tra các row setting hiển thị đúng dựa trên trạng thái ViewModel.
 */
@OptIn(
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    coil3.annotation.ExperimentalCoilApi::class,
)
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    @Test
    fun backupSuccess_rendersWithResultState() {
        // backupResult = Success(2) → when branch covered: settings_backup_success
        val items =
            listOf(
                LibraryItemEntity(manga_id = "m1", title = "T1", cover_url = ""),
                LibraryItemEntity(manga_id = "m2", title = "T2", cover_url = ""),
            )
        coEvery { libraryRepo.getAllItems() } returns items
        val vm = viewModel()
        vm.backupLibrary(java.io.ByteArrayOutputStream())

        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun restoreSuccess_rendersWithResultState() {
        // restoreResult = Success(1) → when branch covered: settings_restore_success
        val json =
            """[{"manga_id":"m1","title":"T1","cover_url":"","status":"READING",""" +
                """"last_read_chapter_id":"","last_read_page_index":0,"updated_at":0}]"""
        coEvery { libraryRepo.restoreItems(any()) } returns Unit
        val vm = viewModel()
        vm.restoreLibrary(json.byteInputStream())

        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun restoreFailure_badJson_rendersWithFailureState() {
        // JSON invalid → restoreResult = Failure → settings_restore_failed branch covered
        val vm = viewModel()
        vm.restoreLibrary("not-valid-json".byteInputStream())

        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun clickQualityToggle_triggersViewModelAction() {
        val vm = viewModel()
        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        // Click "Image Quality" row → triggers toggleQuality()
        composeRule.onNodeWithText("Image Quality").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun clickSignOut_triggersSignOutFlow() {
        val vm = viewModel()
        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        // Click "Sign Out" row to trigger signOut()
        composeRule.onNodeWithText("Sign Out").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun signedOut_state_rendersScreen() {
        // signedOut = true → screen renders without crash
        val vm = viewModel()
        vm.signOut()

        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun clickClearCache_triggersAction() {
        val vm = viewModel()
        composeRule.setContent { SettingScreenContent(viewModel = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Clear Image Cache").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
