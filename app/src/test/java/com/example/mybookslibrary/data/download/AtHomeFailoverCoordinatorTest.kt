package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.repository.ChapterDelivery
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AtHomeFailoverCoordinatorTest {

    @Test
    fun onPageFailure_refreshesAfterThresholdAndUsesNewBaseUrl() = runTest {
        val refreshCount = AtomicInteger(0)
        val coordinator = AtHomeFailoverCoordinator(
            initialDelivery = delivery("https://old-node.example.net"),
            refreshDelivery = {
                refreshCount.incrementAndGet()
                delivery("https://new-node.example.net")
            },
            errorThreshold = 3
        )

        assertFalse(coordinator.onPageFailure(CHAPTER_ID))
        assertFalse(coordinator.onPageFailure(CHAPTER_ID))
        assertTrue(coordinator.onPageFailure(CHAPTER_ID))

        assertEquals(1, refreshCount.get())
        assertEquals(
            "https://new-node.example.net/data/hash/page-1.png",
            coordinator.pageUrl(0)
        )
    }

    @Test
    fun concurrentFailures_triggerSingleRefresh() = runTest {
        val refreshCount = AtomicInteger(0)
        val coordinator = AtHomeFailoverCoordinator(
            initialDelivery = delivery("https://old-node.example.net"),
            refreshDelivery = {
                delay(10)
                refreshCount.incrementAndGet()
                delivery("https://new-node.example.net")
            },
            errorThreshold = 3
        )

        val results = listOf(
            async { coordinator.onPageFailure(CHAPTER_ID) },
            async { coordinator.onPageFailure(CHAPTER_ID) },
            async { coordinator.onPageFailure(CHAPTER_ID) }
        ).awaitAll()

        assertEquals(1, refreshCount.get())
        assertEquals(1, results.count { it })
    }

    @Test
    fun pageSuccess_resetsConsecutiveErrors() = runTest {
        val refreshCount = AtomicInteger(0)
        val coordinator = AtHomeFailoverCoordinator(
            initialDelivery = delivery("https://old-node.example.net"),
            refreshDelivery = {
                refreshCount.incrementAndGet()
                delivery("https://new-node.example.net")
            },
            errorThreshold = 3
        )

        assertFalse(coordinator.onPageFailure(CHAPTER_ID))
        assertFalse(coordinator.onPageFailure(CHAPTER_ID))
        coordinator.onPageSuccess()
        assertFalse(coordinator.onPageFailure(CHAPTER_ID))

        assertEquals(0, refreshCount.get())
    }

    private fun delivery(baseUrl: String): ChapterDelivery = ChapterDelivery(
        baseUrl = baseUrl,
        quality = "data",
        hash = "hash",
        filenames = listOf("page-1.png", "page-2.png")
    )

    private companion object {
        const val CHAPTER_ID = "chapter-1"
    }
}
