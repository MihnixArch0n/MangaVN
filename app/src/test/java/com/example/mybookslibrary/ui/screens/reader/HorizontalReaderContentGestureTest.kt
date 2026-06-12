package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.util.FakeImageLoader
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
class HorizontalReaderContentGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = FakeImageLoader.install()

    @After fun tearDown() = FakeImageLoader.reset()

    @Test
    fun ltr_swipeLeft_advancesPage() {
        var settledPage = -1
        composeRule.setContent {
            val pagerState = rememberPagerState(initialPage = 1, pageCount = { pages.size })
            LaunchedEffect(pagerState.settledPage) {
                settledPage = pagerState.settledPage
            }
            HorizontalReaderContent(
                pages = pages,
                pagerState = pagerState,
                readingMode = ReadingMode.LTR,
                onEvent = {},
            )
        }

        composeRule.onRoot().performTouchInput { swipeLeft() }

        composeRule.waitUntil(timeoutMillis = 5_000) { settledPage == 2 }
    }

    @Test
    fun rtl_swipeRight_advancesPage() {
        var settledPage = -1
        composeRule.setContent {
            val pagerState = rememberPagerState(initialPage = 1, pageCount = { pages.size })
            LaunchedEffect(pagerState.settledPage) {
                settledPage = pagerState.settledPage
            }
            HorizontalReaderContent(
                pages = pages,
                pagerState = pagerState,
                readingMode = ReadingMode.RTL,
                onEvent = {},
            )
        }

        composeRule.onRoot().performTouchInput { swipeRight() }

        composeRule.waitUntil(timeoutMillis = 5_000) { settledPage == 2 }
    }

    private companion object {
        val pages = List(3) { "https://x/page-$it.jpg" }
    }
}
