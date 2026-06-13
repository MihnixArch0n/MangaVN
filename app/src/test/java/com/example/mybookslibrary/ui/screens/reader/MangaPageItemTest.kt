package com.example.mybookslibrary.ui.screens.reader

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.ui.util.FakeImageLoader
import me.saket.telephoto.zoomable.EnabledZoomGestures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class MangaPageItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = FakeImageLoader.install()

    @After fun tearDown() = FakeImageLoader.reset()

    @Test
    fun rendersPage_withContentDescription() {
        composeRule.setContent {
            MangaPageItem(
                imageUrl = "https://example.com/page-1.jpg",
                index = 0,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
        // content description = "Reader page 1" (reader_page_description %1$d)
        composeRule.onNodeWithContentDescription("Reader page 1").assertIsDisplayed()
    }

    @Test
    fun rendersPage2_correctDescription() {
        composeRule.setContent {
            MangaPageItem(
                imageUrl = "https://example.com/page-2.jpg",
                index = 1,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Reader page 2").assertIsDisplayed()
    }

    @Test
    fun rendersLocalFileUri_withoutReaderSchemeError() {
        val localPage =
            File(composeRule.activity.cacheDir, "reader-local-page.png").apply {
                outputStream().use { output ->
                    Bitmap
                        .createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                        .apply { eraseColor(Color.WHITE) }
                        .compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            }
        composeRule.setContent {
            MangaPageItem(
                imageUrl = localPage.toURI().toString(),
                index = 0,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Reader page 1").assertIsDisplayed()
        assertTrue(pageImageData(localPage.toURI().toString()) is File)
        assertEquals("https://example.com/page.jpg", pageImageData("https://example.com/page.jpg"))
        localPage.delete()
    }

    @Test
    fun rendersWithoutCrash_longPressNull() {
        composeRule.setContent {
            MangaPageItem(
                imageUrl = "https://example.com/p.jpg",
                index = 3,
                onLongPress = null,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun parentPageSwipe_atBaseZoom_disablesSingleFingerImagePan() {
        assertEquals(EnabledZoomGestures.ZoomOnly, mangaPageGestures(allowParentPageSwipe = true, userZoom = 0f))
        assertEquals(EnabledZoomGestures.ZoomOnly, mangaPageGestures(allowParentPageSwipe = true, userZoom = 1f))
        assertEquals(EnabledZoomGestures.ZoomOnly, mangaPageGestures(allowParentPageSwipe = true, userZoom = 1.001f))
    }

    @Test
    fun parentPageSwipe_whenZoomed_keepsImagePanEnabled() {
        assertEquals(
            EnabledZoomGestures.ZoomAndPan,
            mangaPageGestures(allowParentPageSwipe = true, userZoom = 1.01f),
        )
        assertEquals(EnabledZoomGestures.ZoomAndPan, mangaPageGestures(allowParentPageSwipe = false, userZoom = 1f))
    }

    @Test
    fun errorState_showsRetryButton() {
        FakeImageLoader.reset()
        FakeImageLoader.installFailing()
        try {
            composeRule.setContent {
                MangaPageItem(imageUrl = "https://x/p.jpg", index = 0)
            }
            composeRule.waitForIdle()
            // isError=true khi Coil fail → "Tap to retry" hoặc BrokenImage icon visible
            composeRule.onNodeWithText("Tap to retry").assertIsDisplayed()
        } finally {
            FakeImageLoader.reset()
            FakeImageLoader.install()
        }
    }

    @Test
    fun loadingState_showsPageLoadingIndicator() {
        FakeImageLoader.reset()
        FakeImageLoader.installPending()
        try {
            composeRule.setContent {
                MangaPageItem(imageUrl = "https://x/pending.jpg", index = 1)
            }
            composeRule.onNodeWithContentDescription("Loading page 2").assertIsDisplayed()
        } finally {
            FakeImageLoader.reset()
            FakeImageLoader.install()
        }
    }
}
