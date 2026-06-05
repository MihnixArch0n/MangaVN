package com.example.mybookslibrary.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Kiểm tra route string và argument name của tất cả navigation destinations.
 * Dùng Robolectric vì createRoute() gọi android.net.Uri.encode.
 * Bắt lỗi sớm nếu ai rename/refactor route mà quên cập nhật deeplink/intent.
 */
@RunWith(RobolectricTestRunner::class)
class NavigationDestinationsTest {
    // ── BottomNavDestination ──────────────────────────────────────────

    @Test
    fun bottomNavDestination_routes_areCorrect() {
        assertEquals("discover", BottomNavDestination.Discover.route)
        assertEquals("search", BottomNavDestination.Search.route)
        assertEquals("library", BottomNavDestination.Library.route)
        assertEquals("setting", BottomNavDestination.Setting.route)
    }

    @Test
    fun bottomNavDestination_allFour_areUnique() {
        val routes =
            listOf(
                BottomNavDestination.Discover.route,
                BottomNavDestination.Search.route,
                BottomNavDestination.Library.route,
                BottomNavDestination.Setting.route,
            )
        assertEquals(routes.size, routes.toSet().size)
    }

    // ── AuthDestination ───────────────────────────────────────────────

    @Test
    fun authDestination_routes_areCorrect() {
        assertEquals("login", AuthDestination.Login)
        assertEquals("register", AuthDestination.Register)
    }

    // ── ReaderDestination ─────────────────────────────────────────────

    @Test
    fun readerDestination_routePattern_containsBaseRoute() {
        assertTrue(ReaderDestination.routePattern.startsWith(ReaderDestination.route))
    }

    @Test
    fun readerDestination_createRoute_encodesSpecialChars() {
        val route =
            ReaderDestination.createRoute(
                mangaId = "abc/123",
                chapterId = "ch 1",
                chapterTitle = "Chapter & One",
                startPageIndex = 5,
            )
        assertTrue(route.startsWith("reader/"))
        assertTrue(route.endsWith("/5"))
    }

    @Test
    fun readerDestination_argumentNames_matchRoutePattern() {
        assertTrue(ReaderDestination.routePattern.contains(ReaderDestination.mangaIdArgumentName))
        assertTrue(ReaderDestination.routePattern.contains(ReaderDestination.chapterIdArgumentName))
        assertTrue(ReaderDestination.routePattern.contains(ReaderDestination.chapterTitleArgumentName))
        assertTrue(ReaderDestination.routePattern.contains(ReaderDestination.startPageIndexArgumentName))
    }

    // ── MangaDetailDestination ────────────────────────────────────────

    @Test
    fun mangaDetailDestination_routePattern_containsBaseRoute() {
        assertTrue(MangaDetailDestination.routePattern.startsWith(MangaDetailDestination.route))
    }

    @Test
    fun mangaDetailDestination_createRoute_truncatesLongDescription() {
        val route =
            MangaDetailDestination.createRoute(
                mangaId = "id1",
                title = "Title",
                coverArt = null,
                description = "x".repeat(700),
                tags = emptyList(),
            )
        assertTrue(route.contains(MangaDetailDestination.route))
    }

    @Test
    fun mangaDetailDestination_createRoute_limitsTags() {
        val route =
            MangaDetailDestination.createRoute(
                mangaId = "id1",
                title = "T",
                coverArt = null,
                description = "d",
                tags = listOf("a", "b", "c", "d", "e"),
            )
        assertTrue(route.contains(MangaDetailDestination.route))
    }

    @Test
    fun mangaDetailDestination_argumentNames_matchRoutePattern() {
        assertTrue(MangaDetailDestination.routePattern.contains(MangaDetailDestination.mangaIdArgumentName))
        assertTrue(MangaDetailDestination.routePattern.contains(MangaDetailDestination.titleArgumentName))
        assertTrue(MangaDetailDestination.routePattern.contains(MangaDetailDestination.coverArtArgumentName))
        assertTrue(MangaDetailDestination.routePattern.contains(MangaDetailDestination.descriptionArgumentName))
        assertTrue(MangaDetailDestination.routePattern.contains(MangaDetailDestination.tagsArgumentName))
    }

    // ── MangaReviewDestination ────────────────────────────────────────

    @Test
    fun mangaReviewDestination_routePattern_containsBaseRoute() {
        assertTrue(MangaReviewDestination.routePattern.startsWith(MangaReviewDestination.route))
    }

    @Test
    fun mangaReviewDestination_createRoute_encodesId() {
        val route = MangaReviewDestination.createRoute("manga/id 1")
        assertTrue(route.startsWith("manga_review/"))
    }

    @Test
    fun mangaReviewDestination_argumentName_matchesRoutePattern() {
        assertTrue(MangaReviewDestination.routePattern.contains(MangaReviewDestination.mangaIdArgumentName))
    }
}
