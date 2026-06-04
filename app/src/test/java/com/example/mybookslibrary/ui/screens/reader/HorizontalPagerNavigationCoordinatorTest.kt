package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.domain.model.ReaderTapAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HorizontalPagerNavigationCoordinatorTest {

    @Test
    fun `second tap retargets active animation without waiting for first animation to finish`() = runTest {
        val harness = CoordinatorHarness(this)

        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), harness.requests.tryReceive().getOrNull())

        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(
            AnimationRequest(page = 2, pendingTargetPage = 2, isRetargeted = true),
            harness.requests.tryReceive().getOrNull()
        )

        harness.releaseAnimation()
        runCurrent()
        assertEquals(2, harness.currentPage)
    }

    @Test
    fun `reverse tap reduces queued forward destination by one page`() = runTest {
        val harness = CoordinatorHarness(this)

        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), harness.requests.tryReceive().getOrNull())

        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(
            AnimationRequest(page = 2, pendingTargetPage = 2, isRetargeted = true),
            harness.requests.tryReceive().getOrNull()
        )

        harness.coordinator.enqueue(ReaderTapAction.PREVIOUS_PAGE)
        runCurrent()
        assertEquals(
            AnimationRequest(page = 1, pendingTargetPage = 1, isRetargeted = true),
            harness.requests.tryReceive().getOrNull()
        )

        harness.releaseAnimation()
        runCurrent()

        assertEquals(1, harness.currentPage)
        assertNull(harness.requests.tryReceive().getOrNull())
    }

    @Test
    fun `queue clamps at final chapter page`() = runTest {
        val harness = CoordinatorHarness(this, initialPage = 1, lastPageIndex = 2)

        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(AnimationRequest(page = 2, pendingTargetPage = 2), harness.requests.tryReceive().getOrNull())

        harness.releaseAnimation()
        runCurrent()
        assertEquals(2, harness.currentPage)
        assertNull(harness.requests.tryReceive().getOrNull())
    }

    @Test
    fun `interrupted animation keeps queue so next edge tap resumes navigation`() = runTest {
        val requests = Channel<AnimationRequest>(Channel.UNLIMITED)
        var currentPage = 0
        var shouldInterrupt = true
        val coordinator = HorizontalPagerNavigationCoordinator(
            scope = this,
            currentPage = { currentPage },
            lastPageIndex = { 7 },
            animateToPage = { nextPage, pendingTargetPage, isRetargeted ->
                requests.send(AnimationRequest(nextPage, pendingTargetPage, isRetargeted))
                if (shouldInterrupt) {
                    shouldInterrupt = false
                    throw CancellationException("Pointer input interrupted animation")
                }
                currentPage = nextPage
            }
        )

        coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), requests.tryReceive().getOrNull())

        coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(AnimationRequest(page = 2, pendingTargetPage = 2), requests.tryReceive().getOrNull())
        assertEquals(2, currentPage)
    }

    @Test
    fun `manual drag clears queue and cancels active worker`() = runTest {
        val harness = CoordinatorHarness(this)

        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()
        assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), harness.requests.tryReceive().getOrNull())

        harness.coordinator.cancelPendingNavigation()
        runCurrent()
        harness.currentPage = 3
        harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
        runCurrent()

        assertEquals(AnimationRequest(page = 4, pendingTargetPage = 4), harness.requests.tryReceive().getOrNull())
        harness.coordinator.cancelPendingNavigation()
        runCurrent()
    }

    private class CoordinatorHarness(
        scope: kotlinx.coroutines.CoroutineScope,
        initialPage: Int = 0,
        private val lastPageIndex: Int = 7
    ) {
        val requests = Channel<AnimationRequest>(Channel.UNLIMITED)
        private val releases = Channel<Unit>(Channel.UNLIMITED)
        var currentPage = initialPage
        val coordinator = HorizontalPagerNavigationCoordinator(
            scope = scope,
            currentPage = { currentPage },
            lastPageIndex = { lastPageIndex },
            animateToPage = { page, pendingTargetPage, isRetargeted ->
                requests.send(AnimationRequest(page, pendingTargetPage, isRetargeted))
                releases.receive()
                currentPage = page
            }
        )

        fun releaseAnimation() {
            releases.trySend(Unit)
        }
    }

    private data class AnimationRequest(
        val page: Int,
        val pendingTargetPage: Int,
        val isRetargeted: Boolean = false
    )
}
