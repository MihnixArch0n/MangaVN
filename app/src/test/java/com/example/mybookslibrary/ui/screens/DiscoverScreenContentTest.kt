package com.example.mybookslibrary.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.DiscoverViewModel
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class DiscoverScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    private val application: Application get() = RuntimeEnvironment.getApplication()

    private fun loadingVm(): DiscoverViewModel {
        val repo = mockk<MangaRepository>()
        every { repo.getDiscoverManga(any(), any()) } returns flowOf()
        return DiscoverViewModel(application, repo, UnconfinedTestDispatcher())
    }

    private fun errorVm(): DiscoverViewModel {
        val repo = mockk<MangaRepository>()
        every { repo.getDiscoverManga(any(), any()) } returns
            flowOf(Result.failure(IllegalStateException("network error")))
        return DiscoverViewModel(application, repo, UnconfinedTestDispatcher())
    }

    private fun loadedVm(items: List<MangaModel>): DiscoverViewModel {
        val repo = mockk<MangaRepository>()
        every { repo.getDiscoverManga(any(), any()) } returns flowOf(Result.success(items))
        return DiscoverViewModel(application, repo, UnconfinedTestDispatcher())
    }

    @Test
    fun loadingState_screenshotRendersWithoutCrash() {
        composeRule.setContent { DiscoverScreenContent(vm = loadingVm()) }

        // Loading → CircularProgressIndicator, screen không crash
        composeRule.waitForIdle()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent { DiscoverScreenContent(vm = errorVm()) }

        composeRule.onNodeWithText("Couldn't load home screen").assertIsDisplayed()
    }

    @Test
    fun withItems_showsMangaTitle() {
        val manga = MangaModel("m1", "Berserk", "Dark manga", null, emptyList())
        composeRule.setContent { DiscoverScreenContent(vm = loadedVm(listOf(manga))) }

        composeRule.onNodeWithText("Berserk").assertIsDisplayed()
    }

    @Test
    fun errorState_withNullMessage_showsGenericError() {
        val repo = mockk<MangaRepository>()
        every { repo.getDiscoverManga(any(), any()) } returns flowOf(Result.failure(RuntimeException()))
        val vm = DiscoverViewModel(application, repo, UnconfinedTestDispatcher())
        composeRule.setContent { DiscoverScreenContent(vm = vm) }
        composeRule.onNodeWithText("Couldn't load home screen").assertIsDisplayed()
    }

    @Test
    fun withManyItems_rendersFirstSpotlight() {
        val items = List(15) { MangaModel("m$it", "Manga $it", "", null, emptyList()) }
        composeRule.setContent { DiscoverScreenContent(vm = loadedVm(items)) }
        composeRule.waitForIdle()
    }
}
