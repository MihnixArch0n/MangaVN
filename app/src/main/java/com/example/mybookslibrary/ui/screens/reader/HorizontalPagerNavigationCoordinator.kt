package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.domain.model.ReaderTapAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Coalesces horizontal pager navigation so consecutive taps retarget the active animation
 * instead of waiting for each adjacent page animation to finish.
 */
internal class HorizontalPagerNavigationCoordinator(
    private val scope: CoroutineScope,
    private val currentPage: () -> Int,
    private val lastPageIndex: () -> Int,
    private val animateToPage: suspend (page: Int, pendingTargetPage: Int, isRetargeted: Boolean) -> Unit
) {
    private var pendingTargetPage: Int? = null
    private var navigationJob: Job? = null
    private var navigationGeneration = 0

    fun enqueue(action: ReaderTapAction) {
        val basePage = pendingTargetPage ?: currentPage()
        val nextTargetPage = calculateHorizontalTargetPage(
            targetPage = basePage,
            action = action,
            lastPageIndex = lastPageIndex()
        ) ?: return

        Timber.d(
            "Reader pager retarget enqueue: action=%s current=%d pending=%s base=%d nextTarget=%d active=%s",
            action,
            currentPage(),
            pendingTargetPage?.toString() ?: "<none>",
            basePage,
            nextTargetPage,
            navigationJob?.isActive == true
        )
        if (nextTargetPage == basePage) {
            Timber.d("Reader pager retarget ignored at boundary: page=%d action=%s", basePage, action)
            return
        }
        val isRetargeted = navigationJob?.isActive == true
        pendingTargetPage = nextTargetPage
        launchNavigationTo(nextTargetPage, isRetargeted)
    }

    fun cancelPendingNavigation() {
        Timber.d(
            "Reader pager retarget cleared by drag: current=%d pending=%s active=%s",
            currentPage(),
            pendingTargetPage?.toString() ?: "<none>",
            navigationJob?.isActive == true
        )
        pendingTargetPage = null
        navigationGeneration++
        navigationJob?.cancel()
        navigationJob = null
    }

    private fun launchNavigationTo(targetPage: Int, isRetargeted: Boolean) {
        val generation = ++navigationGeneration
        navigationJob?.cancel()

        val job = scope.launch(start = CoroutineStart.LAZY) {
            val page = currentPage()
            if (page == targetPage || lastPageIndex() < 0) {
                pendingTargetPage = null
                return@launch
            }

            Timber.d("Reader pager retarget animation start: current=%d target=%d generation=%d", page, targetPage, generation)
            try {
                animateToPage(targetPage, targetPage, isRetargeted)
                Timber.d(
                    "Reader pager retarget animation end: current=%d target=%d pending=%s generation=%d",
                    currentPage(),
                    targetPage,
                    pendingTargetPage?.toString() ?: "<none>",
                    generation
                )
                if (pendingTargetPage == targetPage) {
                    pendingTargetPage = null
                }
            } catch (cancellation: CancellationException) {
                Timber.d(
                    cancellation,
                    "Reader pager retarget animation interrupted: current=%d target=%d pending=%s generation=%d",
                    currentPage(),
                    targetPage,
                    pendingTargetPage?.toString() ?: "<none>",
                    generation
                )
            } finally {
                if (navigationGeneration == generation) {
                    navigationJob = null
                }
            }
        }
        navigationJob = job
        job.start()
    }
}
