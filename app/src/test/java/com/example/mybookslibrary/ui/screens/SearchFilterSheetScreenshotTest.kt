package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.mybookslibrary.domain.model.MangaTag
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.viewmodel.SearchUiState
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot test: capture chỉ active khi chạy record/verifyRoborazziDebug (CI).
// ModalBottomSheet render trong window riêng → dùng captureScreenRoboImage
// (chụp cả màn hình gồm dialog/popup) thay vì onRoot().captureRoboImage().
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchFilterSheetScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun filterSheet_withSelections_light() {
        capture(darkTheme = false)
    }

    @Test
    fun filterSheet_withSelections_dark() {
        capture(darkTheme = true)
    }

    private fun capture(darkTheme: Boolean) {
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = darkTheme) {
                SearchFilterSheet(
                    state = stateWithFilters(),
                    onToggleTag = {},
                    onToggleLanguage = {},
                    onToggleContentRating = {},
                    onToggleStatus = {},
                    onClearFilters = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
        captureScreenRoboImage()
    }

    private fun stateWithFilters() =
        SearchUiState(
            availableGenres =
                listOf(
                    MangaTag(id = "t1", name = "Action", group = "genre"),
                    MangaTag(id = "t2", name = "Romance", group = "genre"),
                    MangaTag(id = "t3", name = "Comedy", group = "genre"),
                ),
            availableThemes = listOf(MangaTag(id = "t4", name = "School Life", group = "theme")),
            selectedTagIds = setOf("t1"),
            selectedLanguages = setOf("en"),
            isFilterSheetOpen = true,
            activeFilterCount = 2,
        )
}
