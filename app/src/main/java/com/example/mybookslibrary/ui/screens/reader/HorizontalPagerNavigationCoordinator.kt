package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.domain.model.ReaderTapAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Queues horizontal pager navigation so consecutive taps still render each intermediate
 * page transition instead of cancelling the active animation and jumping to the final page.
 */
internal class HorizontalPagerNavigationCoordinator(
    private val scope: CoroutineScope,
    private val currentPage: () -> Int,
    private val lastPageIndex: () -> Int,
    private val animateToPage: suspend (page: Int, pendingTargetPage: Int, isQueuedNavigation: Boolean) -> Unit,
    private val onNavigationActiveChanged: (Boolean) -> Unit = {},
) {
    private var pendingTargetPage: Int? = null
    private var navigationJob: Job? = null
    private var hasQueuedNavigation = false

    fun enqueue(action: ReaderTapAction) {
        val basePage = pendingTargetPage ?: currentPage()
        val nextTargetPage =
            calculateHorizontalTargetPage(
                targetPage = basePage,
                action = action,
                lastPageIndex = lastPageIndex(),
            )

        if (nextTargetPage != null) {
            Timber.v(
                "Reader pager queue enqueue: action=%s current=%d pending=%s base=%d nextTarget=%d active=%s",
                action,
                currentPage(),
                pendingTargetPage?.toString() ?: "<none>",
                basePage,
                nextTargetPage,
                navigationJob?.isActive == true,
            )
            if (nextTargetPage == basePage) {
                Timber.v(
                    "Reader pager queue ignored at boundary: page=%d action=%s",
                    basePage,
                    action,
                )
            } else {
                enqueueTargetPage(nextTargetPage)
            }
        }
    }

    private fun enqueueTargetPage(nextTargetPage: Int) {
        val hadPendingTarget = pendingTargetPage != null
        pendingTargetPage = nextTargetPage
        if (navigationJob?.isActive == true || hadPendingTarget) {
            hasQueuedNavigation = true
        }
        if (navigationJob?.isActive == true) {
            Timber.v("Reader pager queue extended: pending=%d", nextTargetPage)
            return
        }
        launchNavigationWorker()
    }

    fun cancelPendingNavigation() {
        Timber.v(
            "Reader pager queue cleared by drag: current=%d pending=%s active=%s",
            currentPage(),
            pendingTargetPage?.toString() ?: "<none>",
            navigationJob?.isActive == true,
        )
        pendingTargetPage = null
        hasQueuedNavigation = false
        navigationJob?.cancel()
        navigationJob = null
        onNavigationActiveChanged(false)
    }

    private fun launchNavigationWorker() {
        navigationJob =
            scope.launch {
                Timber.v(
                    "Reader pager queue worker start: pending=%s",
                    pendingTargetPage?.toString() ?: "<none>",
                )
                onNavigationActiveChanged(true)
                try {
                    animatePendingPages()
                } finally {
                    Timber.v(
                        "Reader pager queue worker end: pending=%s",
                        pendingTargetPage?.toString() ?: "<none>",
                    )
                    navigationJob = null
                    onNavigationActiveChanged(false)
                }
            }
    }

    private suspend fun animatePendingPages() {
        while (pendingTargetPage != null && lastPageIndex() >= 0) {
            val targetPage = pendingTargetPage ?: break
            val page = currentPage()
            if (page == targetPage) {
                pendingTargetPage = null
                hasQueuedNavigation = false
            } else {
                animateNextPage(targetPage = targetPage, page = page)
            }
        }
    }

    private suspend fun animateNextPage(
        targetPage: Int,
        page: Int,
    ) {
        val nextPage = if (targetPage > page) page + 1 else page - 1
        val isQueuedNavigation = hasQueuedNavigation
        Timber.v(
            "Reader pager queue animation start: current=%d next=%d pending=%d queued=%s",
            page,
            nextPage,
            targetPage,
            isQueuedNavigation,
        )
        try {
            animateToPage(nextPage, targetPage, isQueuedNavigation)
            Timber.v(
                "Reader pager queue animation end: current=%d next=%d pending=%s",
                currentPage(),
                nextPage,
                pendingTargetPage?.toString() ?: "<none>",
            )
        } catch (cancellation: CancellationException) {
            Timber.v(
                cancellation,
                "Reader pager queue animation interrupted: current=%d next=%d pending=%s",
                currentPage(),
                nextPage,
                pendingTargetPage?.toString() ?: "<none>",
            )
            pendingTargetPage = null
            hasQueuedNavigation = false
        }
    }
}
