package com.example.mybookslibrary.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavigationDestinationsTest {
    @Test
    fun bottomNavDestinations_haveUniqueTypedDestinations() {
        val destinations = bottomDestinations.map { it.destination }

        assertEquals(destinations.size, destinations.toSet().size)
    }

    @Test
    fun mangaDetail_containsOnlyMangaId() {
        assertEquals(MangaDetail(mangaId = "manga-1"), MangaDetail(mangaId = "manga-1"))
        assertNotEquals(MangaDetail(mangaId = "manga-1"), MangaDetail(mangaId = "manga-2"))
    }

    @Test
    fun reader_preservesAllArguments() {
        val route =
            Reader(
                mangaId = "manga-1",
                chapterId = "chapter-1",
                chapterTitle = "Chapter 1",
                startPageIndex = 5,
            )

        assertEquals("manga-1", route.mangaId)
        assertEquals("chapter-1", route.chapterId)
        assertEquals("Chapter 1", route.chapterTitle)
        assertEquals(5, route.startPageIndex)
    }

    @Test
    fun authAndMainDestinations_areDistinct() {
        assertNotEquals(Login, Register)
        assertNotEquals(Discover, Search)
        assertNotEquals(Library, Setting)
    }
}
