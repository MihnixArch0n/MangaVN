package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
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
class MangaDetailSectionsScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    @Test
    fun detailSections_light() {
        capture(darkTheme = false)
    }

    @Test
    fun detailSections_dark() {
        capture(darkTheme = true)
    }

    private fun capture(darkTheme: Boolean) {
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = darkTheme) {
                Column(Modifier.fillMaxSize()) {
                    PublisherSection(
                        description = "Một bộ manga kinh điển về hành trình trưởng thành của kiếm sĩ trẻ.",
                    )
                    FirstChapterPreviewSection(pageUrls = listOf("", "", ""))
                    CustomerReviewsSection(onReviewClick = {})
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }
}
