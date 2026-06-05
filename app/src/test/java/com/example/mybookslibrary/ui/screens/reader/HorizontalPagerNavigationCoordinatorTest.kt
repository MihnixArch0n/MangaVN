package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.domain.model.ReaderTapAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HorizontalPagerNavigationCoordinatorTest {
    @Test
    fun `second tap queues a second adjacent animation after the active animation finishes`() =
        runTest {
            val harness = CoordinatorHarness(this)

            harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), harness.requests.tryReceive().getOrNull())

            harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertNull(harness.requests.tryReceive().getOrNull())

            harness.releaseAnimation()
            runCurrent()
            assertEquals(
                AnimationRequest(page = 2, pendingTargetPage = 2, isQueuedNavigation = true),
                harness.requests.tryReceive().getOrNull(),
            )

            harness.releaseAnimation()
            runCurrent()
            assertEquals(2, harness.currentPage)
        }

    @Test
    fun `reverse tap reduces queued forward destination by one page`() =
        runTest {
            val harness = CoordinatorHarness(this)

            harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), harness.requests.tryReceive().getOrNull())

            harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertNull(harness.requests.tryReceive().getOrNull())

            harness.coordinator.enqueue(ReaderTapAction.PREVIOUS_PAGE)
            runCurrent()
            assertNull(harness.requests.tryReceive().getOrNull())

            harness.releaseAnimation()
            runCurrent()

            assertEquals(1, harness.currentPage)
            assertNull(harness.requests.tryReceive().getOrNull())
        }

    @Test
    fun `queue clamps at final chapter page`() =
        runTest {
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
    fun `interrupted animation clears stale queue so next edge tap starts from current page`() =
        runTest {
            val requests = Channel<AnimationRequest>(Channel.UNLIMITED)
            var currentPage = 0
            var shouldInterrupt = true
            val coordinator =
                HorizontalPagerNavigationCoordinator(
                    scope = this,
                    currentPage = { currentPage },
                    lastPageIndex = { 7 },
                    animateToPage = { nextPage, pendingTargetPage, isQueuedNavigation ->
                        requests.send(AnimationRequest(nextPage, pendingTargetPage, isQueuedNavigation))
                        if (shouldInterrupt) {
                            shouldInterrupt = false
                            throw CancellationException("Pointer input interrupted animation")
                        }
                        currentPage = nextPage
                    },
                )

            coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), requests.tryReceive().getOrNull())

            coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertEquals(
                AnimationRequest(page = 1, pendingTargetPage = 1),
                requests.tryReceive().getOrNull(),
            )
            assertNull(requests.tryReceive().getOrNull())
            assertEquals(1, currentPage)
        }

    @Test
    fun `manual drag clears queue and cancels active worker`() =
        runTest {
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

    @Test
    fun `navigation active state wraps animation lifecycle`() =
        runTest {
            val activeStates = mutableListOf<Boolean>()
            val harness =
                CoordinatorHarness(
                    scope = this,
                    onNavigationActiveChanged = activeStates::add,
                )

            assertFalse(activeStates.lastOrNull() == true)

            harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()

            assertTrue(activeStates.last())
            assertEquals(AnimationRequest(page = 1, pendingTargetPage = 1), harness.requests.tryReceive().getOrNull())

            harness.releaseAnimation()
            runCurrent()

            assertEquals(listOf(true, false), activeStates)
        }

    @Test
    fun `cancel pending navigation clears active state`() =
        runTest {
            val activeStates = mutableListOf<Boolean>()
            val harness =
                CoordinatorHarness(
                    scope = this,
                    onNavigationActiveChanged = activeStates::add,
                )

            harness.coordinator.enqueue(ReaderTapAction.NEXT_PAGE)
            runCurrent()
            assertTrue(activeStates.last())

            harness.coordinator.cancelPendingNavigation()
            runCurrent()

            assertFalse(activeStates.last())
        }

    private class CoordinatorHarness(
        scope: kotlinx.coroutines.CoroutineScope,
        initialPage: Int = 0,
        private val lastPageIndex: Int = 7,
        onNavigationActiveChanged: (Boolean) -> Unit = {},
    ) {
        val requests = Channel<AnimationRequest>(Channel.UNLIMITED)
        private val releases = Channel<Unit>(Channel.UNLIMITED)
        var currentPage = initialPage
        val coordinator =
            HorizontalPagerNavigationCoordinator(
                scope = scope,
                currentPage = { currentPage },
                lastPageIndex = { lastPageIndex },
                animateToPage = { page, pendingTargetPage, isQueuedNavigation ->
                    requests.send(AnimationRequest(page, pendingTargetPage, isQueuedNavigation))
                    releases.receive()
                    currentPage = page
                },
                onNavigationActiveChanged = onNavigationActiveChanged,
            )

        fun releaseAnimation() {
            releases.trySend(Unit)
        }
    }

    private data class AnimationRequest(
        val page: Int,
        val pendingTargetPage: Int,
        val isQueuedNavigation: Boolean = false,
    )
}
