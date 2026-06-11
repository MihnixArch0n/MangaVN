package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot test: capture chỉ active khi chạy record/verifyRoborazziDebug (CI).
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class DiscoverScreenScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    @Test
    fun discoverContent_light() {
        capture(darkTheme = false)
    }

    @Test
    fun discoverContent_dark() {
        capture(darkTheme = true)
    }

    private fun capture(darkTheme: Boolean) {
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = darkTheme) {
                DiscoverContentList(
                    contentPadding = PaddingValues(),
                    spotlight = manga("spotlight", "Vagabond"),
                    popularItems = listOf(manga("p1", "Naruto"), manga("p2", "One Piece")),
                    newItems = listOf(manga("n1", "Frieren")),
                    exploreItems = listOf(manga("e1", "Berserk")),
                    expandedPopular = false,
                    expandedNew = false,
                    expandedExplore = false,
                    onTogglePopular = {},
                    onToggleNew = {},
                    onToggleExplore = {},
                    onMangaClick = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    private fun manga(
        id: String,
        title: String,
    ) = MangaModel(
        id = id,
        title = title,
        description = "Mô tả cố định cho screenshot test.",
        coverArt = "",
        tags = listOf("Action", "Adventure"),
    )
}
