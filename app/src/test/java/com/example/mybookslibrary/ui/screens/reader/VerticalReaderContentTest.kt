package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class VerticalReaderContentTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = FakeImageLoader.install()

    @After fun tearDown() = FakeImageLoader.reset()

    @Test
    fun rendersWithSinglePage() {
        composeRule.setContent {
            VerticalReaderContent(
                pages = listOf("https://x/p0.jpg"),
                listState = rememberLazyListState(),
                onEvent = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rendersWithMultiplePages() {
        composeRule.setContent {
            VerticalReaderContent(
                pages = List(5) { "https://x/p$it.jpg" },
                listState = rememberLazyListState(),
                onEvent = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rendersEmptyPages_doesNotCrash() {
        composeRule.setContent {
            VerticalReaderContent(
                pages = emptyList(),
                listState = rememberLazyListState(),
                onEvent = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun onEvent_canBeDispatched() {
        var received: ReaderEvent? = null
        composeRule.setContent {
            VerticalReaderContent(
                pages = listOf("https://x/p0.jpg"),
                listState = rememberLazyListState(),
                onEvent = { received = it },
            )
        }
        composeRule.waitForIdle()
        // Test chỉ verify không crash — received luôn null vì chưa trigger event
    }

    @Test
    fun doubleTap_isHandledByZoom_withoutTogglingOverlay() {
        var received: ReaderEvent? = null
        var zoomFraction: Float? = null
        composeRule.setContent {
            VerticalReaderContent(
                pages = listOf("https://x/p0.jpg"),
                listState = rememberLazyListState(),
                onEvent = { received = it },
                onZoomFractionChanged = { zoomFraction = it },
            )
        }

        composeRule.onRoot().performTouchInput { doubleClick() }
        composeRule.waitUntil(timeoutMillis = 5_000) { zoomFraction?.let { it > 0f } == true }

        assertNull(received)
        assertTrue(zoomFraction!! > 0f)
    }

    @Test
    fun singleTap_stillDispatchesTapEvent() {
        var received: ReaderEvent? = null
        composeRule.setContent {
            VerticalReaderContent(
                pages = listOf("https://x/p0.jpg"),
                listState = rememberLazyListState(),
                onEvent = { received = it },
            )
        }

        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitUntil(timeoutMillis = 5_000) { received != null }

        assertTrue(received is ReaderEvent.TapOnScreen)
    }
}
