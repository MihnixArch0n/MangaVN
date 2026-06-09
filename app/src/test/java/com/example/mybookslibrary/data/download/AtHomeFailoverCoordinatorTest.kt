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
    fun onPageFailure_refreshesGenerationAndUsesNewBaseUrl() =
        runTest {
            val refreshCount = AtomicInteger(0)
            val coordinator =
                AtHomeFailoverCoordinator(
                    initialDelivery = delivery("https://old-node.example.net"),
                    refreshDelivery = {
                        refreshCount.incrementAndGet()
                        delivery("https://new-node.example.net")
                    },
                )
            val oldAttempt = coordinator.pageAttempt(0)

            assertTrue(coordinator.onPageFailure(CHAPTER_ID, oldAttempt.generation))

            assertEquals(1, refreshCount.get())
            assertEquals(
                "https://new-node.example.net/data/hash/page-1.png",
                coordinator.pageAttempt(0).url,
            )
            assertEquals(oldAttempt.generation + 1, coordinator.pageAttempt(0).generation)
        }

    @Test
    fun concurrentFailures_triggerSingleRefresh() =
        runTest {
            val refreshCount = AtomicInteger(0)
            val coordinator =
                AtHomeFailoverCoordinator(
                    initialDelivery = delivery("https://old-node.example.net"),
                    refreshDelivery = {
                        delay(10)
                        refreshCount.incrementAndGet()
                        delivery("https://new-node.example.net")
                    },
                )
            val failedGeneration = coordinator.pageAttempt(0).generation

            val results =
                listOf(
                    async { coordinator.onPageFailure(CHAPTER_ID, failedGeneration) },
                    async { coordinator.onPageFailure(CHAPTER_ID, failedGeneration) },
                    async { coordinator.onPageFailure(CHAPTER_ID, failedGeneration) },
                ).awaitAll()

            assertEquals(1, refreshCount.get())
            assertEquals(1, results.count { it })
        }

    @Test
    fun staleGenerationFailure_doesNotRefreshReplacementNode() =
        runTest {
            val refreshCount = AtomicInteger(0)
            val coordinator =
                AtHomeFailoverCoordinator(
                    initialDelivery = delivery("https://old-node.example.net"),
                    refreshDelivery = {
                        refreshCount.incrementAndGet()
                        delivery("https://new-node.example.net")
                    },
                )
            val oldGeneration = coordinator.pageAttempt(0).generation

            assertTrue(coordinator.onPageFailure(CHAPTER_ID, oldGeneration))
            assertFalse(coordinator.onPageFailure(CHAPTER_ID, oldGeneration))

            assertEquals(1, refreshCount.get())
        }

    private fun delivery(baseUrl: String): ChapterDelivery =
        ChapterDelivery(
            baseUrl = baseUrl,
            quality = "data",
            hash = "hash",
            filenames = listOf("page-1.png", "page-2.png"),
        )

    private companion object {
        const val CHAPTER_ID = "chapter-1"
    }
}
