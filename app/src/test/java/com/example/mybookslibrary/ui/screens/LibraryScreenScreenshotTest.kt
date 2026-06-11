package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot test: capture chỉ active khi chạy record/verifyRoborazziDebug (CI);
// chạy testDebugUnitTest thường thì captureRoboImage là no-op.
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class LibraryScreenScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    @Test
    fun emptyLibrary_light() {
        capture(darkTheme = false, vm = emptyVm())
    }

    @Test
    fun emptyLibrary_dark() {
        capture(darkTheme = true, vm = emptyVm())
    }

    @Test
    fun withItems_light() {
        capture(darkTheme = false, vm = vmWithItems("Naruto", "One Piece", "Berserk"))
    }

    @Test
    fun withItems_dark() {
        capture(darkTheme = true, vm = vmWithItems("Naruto", "One Piece", "Berserk"))
    }

    private fun capture(
        darkTheme: Boolean,
        vm: LibraryViewModel,
    ) {
        composeRule.setContent {
            MyBooksLibraryTheme(darkTheme = darkTheme) {
                LibraryScreenContent(vm = vm)
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    private fun emptyVm(): LibraryViewModel {
        val repo = mockk<LibraryRepository>()
        every { repo.observeLibraryItems() } returns flowOf(emptyList())
        return LibraryViewModel(repo, UnconfinedTestDispatcher())
    }

    private fun vmWithItems(vararg titles: String): LibraryViewModel {
        val repo = mockk<LibraryRepository>()
        every { repo.observeLibraryItems() } returns
            flowOf(
                titles.mapIndexed { i, t ->
                    LibraryItemEntity(
                        manga_id = "m$i",
                        title = t,
                        cover_url = "",
                        status = LibraryStatus.READING,
                        // Timestamp cố định để ảnh deterministic giữa các lần record
                        updated_at = 1_000L + i,
                    )
                },
            )
        return LibraryViewModel(repo, UnconfinedTestDispatcher())
    }
}
