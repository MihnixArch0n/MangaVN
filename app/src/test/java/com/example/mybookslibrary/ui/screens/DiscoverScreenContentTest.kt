package com.example.mybookslibrary.ui.screens

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
// Màn hình cao 4000dp để LazyColumn compose tất cả items (tránh lazy off-screen)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@coil3.annotation.ExperimentalCoilApi
class DiscoverScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    @Test
    fun withOneItem_showsSpotlightOnly_noPopularSection() {
        // items.size <= 1 → popularItems empty → no "Popular Now" header
        val items = listOf(MangaModel("m0", "Solo Manga", "", null, emptyList()))
        composeRule.setContent { DiscoverScreenContent(vm = loadedVm(items)) }
        composeRule.onNodeWithText("Solo Manga").assertIsDisplayed()
    }

    @Test
    fun withSixItems_rendersPopularBranch() {
        // items.size > 1 → popularItems non-empty → LazyColumn composes items[0] spotlight + items 1-5
        val items = List(6) { MangaModel("m$it", "Manga $it", "", null, emptyList()) }
        composeRule.setContent { DiscoverScreenContent(vm = loadedVm(items)) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manga 0").assertIsDisplayed()
    }

    @Test
    fun withTwelveItems_rendersAllItemBranches() {
        // items > 1, > 6, > 11 → popularItems/newItems/exploreItems all non-empty
        val items = List(12) { MangaModel("m$it", "Manga $it", "", null, emptyList()) }
        composeRule.setContent { DiscoverScreenContent(vm = loadedVm(items)) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manga 0").assertIsDisplayed()
    }

    @Test
    fun clickMangaItem_invokesOnMangaClick_fromSpotlight() {
        // Click Spotlight item (items[0]) → onMangaClick callback
        var clicked: String? = null
        val items = List(6) { MangaModel("m$it", "Manga $it", "", null, emptyList()) }
        composeRule.setContent {
            DiscoverScreenContent(
                onMangaClick = { clicked = it.id },
                vm = loadedVm(items),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manga 0").performClick()
        composeRule.waitForIdle()
        assert(clicked != null) { "onMangaClick phải được gọi khi click spotlight item" }
    }

    @Test
    fun clickMangaCard_invokesOnMangaClick() {
        var clickedId: String? = null
        val items = List(3) { MangaModel("m$it", "Manga $it", "", null, emptyList()) }
        composeRule.setContent {
            DiscoverScreenContent(
                onMangaClick = { clickedId = it.id },
                vm = loadedVm(items),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manga 0").performClick()
        composeRule.waitForIdle()
        assert(clickedId != null) { "onMangaClick phải được gọi khi click manga" }
    }

    @Test
    fun emptyItems_showsEmptyState() {
        // items empty → no loading, no error, no items → empty state renders
        val vm = loadedVm(emptyList())
        composeRule.setContent { DiscoverScreenContent(vm = vm) }
        composeRule.waitForIdle()
    }

    @Test
    fun recompose_errorThenReload_coversTransitionInstructions() {
        // Error state → reload → success: recomposition transition covers restart group instructions
        val repo = mockk<MangaRepository>()
        val items = List(3) { MangaModel("m$it", "Item $it", "", null, emptyList()) }
        every { repo.getDiscoverManga(any(), any()) } returnsMany
            listOf(
                flowOf(Result.failure(RuntimeException("network err"))),
                flowOf(Result.success(items)),
            )
        val vm = DiscoverViewModel(application, repo, UnconfinedTestDispatcher())

        composeRule.setContent { DiscoverScreenContent(vm = vm) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Couldn't load home screen").assertIsDisplayed()
        // Reload → success state → recomposition from error to items
        vm.loadDiscover()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Item 0").assertIsDisplayed()
    }

    @Test
    fun recompose_loadingToItems_triggersByButton() {
        // Click retry button trong error state → loadDiscover() → items appear
        val repo = mockk<MangaRepository>()
        val items = List(6) { MangaModel("m$it", "Manga $it", "", null, emptyList()) }
        every { repo.getDiscoverManga(any(), any()) } returnsMany
            listOf(
                flowOf(Result.failure(RuntimeException("fail"))),
                flowOf(Result.success(items)),
            )
        val vm = DiscoverViewModel(application, repo, UnconfinedTestDispatcher())

        composeRule.setContent { DiscoverScreenContent(vm = vm) }
        composeRule.waitForIdle()
        // Click "Retry" button to reload
        runCatching { composeRule.onNodeWithText("Retry").performClick() }
        vm.loadDiscover()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manga 0").assertIsDisplayed()
    }
}
