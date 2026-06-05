package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class VerticalReaderContentTest {
    @get:Rule val composeRule = createComposeRule()

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
}
