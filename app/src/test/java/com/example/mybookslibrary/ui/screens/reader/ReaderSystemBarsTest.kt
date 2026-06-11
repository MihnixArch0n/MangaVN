package com.example.mybookslibrary.ui.screens.reader

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderSystemBarsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── findActivity ────────────────────────────────────────────────────────

    @Test
    fun findActivity_componentActivity_returnsSelf() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        assertSame(activity, activity.findActivity())
    }

    @Test
    fun findActivity_contextWrapper_returnsWrappedActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val wrapper = ContextWrapper(activity)
        assertSame(activity, wrapper.findActivity())
    }

    @Test
    fun findActivity_nestedContextWrapper_returnsActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        val inner = ContextWrapper(activity)
        val outer = ContextWrapper(inner)
        assertNotNull(outer.findActivity())
    }

    @Test
    fun findActivity_applicationContext_returnsNull() {
        val appContext = RuntimeEnvironment.getApplication()
        assertNull(appContext.findActivity())
    }

    // ── ConfigureReaderSystemBars smoke tests ───────────────────────────────

    @Test
    fun configureReaderSystemBars_overlayVisibleAndLight_doesNotCrash() {
        composeRule.setContent {
            ConfigureReaderSystemBars(
                activity = null,
                backgroundIsLight = true,
                overlayIsVisible = true,
                overlayIsLight = true,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun configureReaderSystemBars_overlayVisibleNotLight_doesNotCrash() {
        composeRule.setContent {
            ConfigureReaderSystemBars(
                activity = null,
                backgroundIsLight = false,
                overlayIsVisible = true,
                overlayIsLight = false,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun configureReaderSystemBars_overlayNotVisible_doesNotCrash() {
        composeRule.setContent {
            ConfigureReaderSystemBars(
                activity = null,
                backgroundIsLight = true,
                overlayIsVisible = false,
                overlayIsLight = false,
            )
        }
        composeRule.waitForIdle()
    }
}
