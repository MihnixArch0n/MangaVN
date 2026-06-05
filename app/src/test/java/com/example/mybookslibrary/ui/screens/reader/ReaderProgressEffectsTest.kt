package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderProgressEffectsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun ltrMode_doesNotCrash() {
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 3 })
            ReaderProgressEffects(
                state = ReaderState(pages = listOf("p0", "p1", "p2"), currentReadingMode = ReadingMode.LTR),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(null) },
                hasRestoredInitialPage = remember { mutableStateOf(false) },
                onEvent = {},
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun verticalMode_doesNotCrash() {
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 3 })
            ReaderProgressEffects(
                state = ReaderState(pages = listOf("p0", "p1", "p2"), currentReadingMode = ReadingMode.VERTICAL),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(null) },
                hasRestoredInitialPage = remember { mutableStateOf(false) },
                onEvent = {},
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun emptyPages_doesNotCrash() {
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 0 })
            ReaderProgressEffects(
                state = ReaderState(pages = emptyList(), currentReadingMode = ReadingMode.VERTICAL),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(null) },
                hasRestoredInitialPage = remember { mutableStateOf(false) },
                onEvent = {},
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rtlMode_doesNotCrash() {
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 2 })
            ReaderProgressEffects(
                state = ReaderState(pages = listOf("p0", "p1"), currentReadingMode = ReadingMode.RTL),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(null) },
                hasRestoredInitialPage = remember { mutableStateOf(false) },
                onEvent = {},
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun ltrMode_withPages_doesNotTriggerVerticalRestore() {
        // LTR mode → vertical LaunchedEffects return early — covers different code path
        val hasRestored = mutableStateOf(false)
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 3 })
            ReaderProgressEffects(
                state =
                    ReaderState(
                        pages = listOf("p0", "p1", "p2"),
                        currentReadingMode = ReadingMode.LTR,
                        lastReadPageIndex = 1,
                    ),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(null) },
                hasRestoredInitialPage = hasRestored,
                onEvent = {},
            )
        }
        composeRule.waitForIdle()
        // LTR mode → hasRestoredInitialPage remains false (vertical restore không chạy)
        assert(!hasRestored.value) { "LTR mode không trigger vertical restore" }
    }

    @Test
    fun rtlMode_withPages_doesNotTriggerVerticalRestore() {
        val hasRestored = mutableStateOf(false)
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 2 })
            ReaderProgressEffects(
                state =
                    ReaderState(
                        pages = listOf("p0", "p1"),
                        currentReadingMode = ReadingMode.RTL,
                    ),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(null) },
                hasRestoredInitialPage = hasRestored,
                onEvent = {},
            )
        }
        composeRule.waitForIdle()
        assert(!hasRestored.value) { "RTL mode không trigger vertical restore" }
    }

    @Test
    fun flushProgress_onDispose_doesNotCrash() {
        val events = mutableListOf<ReaderEvent>()
        composeRule.setContent {
            val listState = rememberLazyListState()
            val pagerState = rememberPagerState(pageCount = { 3 })
            ReaderProgressEffects(
                state = ReaderState(pages = listOf("p0", "p1", "p2")),
                listState = listState,
                pagerState = pagerState,
                latestActivePageIndex = remember { mutableStateOf(2) },
                hasRestoredInitialPage = remember { mutableStateOf(true) },
                onEvent = { events.add(it) },
            )
        }
        composeRule.waitForIdle()
    }
}
