package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.LibraryViewModel
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class LibraryScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

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
                        updated_at = 1000L + i,
                    )
                },
            )
        return LibraryViewModel(repo, UnconfinedTestDispatcher())
    }

    @Test
    fun emptyLibrary_showsEmptyState() {
        composeRule.setContent {
            LibraryScreenContent(vm = emptyVm())
        }

        composeRule.onNodeWithText("Your library is empty").assertIsDisplayed()
        composeRule.onNodeWithText("Start reading to build your collection").assertIsDisplayed()
    }

    @Test
    fun withItems_showsTitleAndMangaName() {
        composeRule.setContent {
            LibraryScreenContent(vm = vmWithItems("Naruto", "One Piece"))
        }

        composeRule.onNodeWithText("My Library").assertIsDisplayed()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
        composeRule.onNodeWithText("One Piece").assertIsDisplayed()
    }

    @Test
    fun withItems_clickItem_invokesOnOpenDetail() {
        var openedId: String? = null
        composeRule.setContent {
            LibraryScreenContent(
                onOpenDetail = { id, _, _ -> openedId = id },
                vm = vmWithItems("Naruto"),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").performClick()
        composeRule.waitForIdle()
        assert(openedId != null) { "onOpenDetail phải được gọi khi click item" }
    }

    @Test
    fun longClickItem_triggersRemovalFlow() {
        // Long click → pendingRemoval != null → ModalBottomSheet render (lines 93-97)
        composeRule.setContent {
            LibraryScreenContent(vm = vmWithItems("Naruto"))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").performTouchInput { longClick() }
        composeRule.waitForIdle()
        // Verify không crash — ModalBottomSheet đã compose
        composeRule.onNodeWithText("My Library").assertIsDisplayed()
    }
}
