package com.example.mybookslibrary.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageFileBuilderTest {
    private val builder = ReaderPageFileBuilder()

    @Test
    fun invoke_normalizesChapterTitleToSlug() {
        val result = builder("Chapter 1: The End!", target("https://cdn.example.com/data/abc/page1.png", 0))

        assertTrue(result.fileName.startsWith("chapter_1_the_end_p1_"))
    }

    @Test
    fun invoke_fallsBackToChapterWhenTitleHasNoAlphanumerics() {
        val result = builder("!!!", target("https://cdn.example.com/page.png", 0))

        assertTrue(result.fileName.startsWith("chapter_p1_"))
    }

    @Test
    fun invoke_extractsExtensionIgnoringQueryAndFragment() {
        val result = builder("One", target("https://cdn.example.com/p/page2.webp?token=abc#frag", 1))

        assertEquals("webp", result.extension)
    }

    @Test
    fun invoke_defaultsExtensionToJpgWhenUrlHasNone() {
        val result = builder("One", target("https://cdn.example.com/pages/raw-page", 0))

        assertEquals("jpg", result.extension)
    }

    @Test
    fun invoke_usesOneBasedPageNumber() {
        val result = builder("One", target("https://cdn.example.com/page.png", 4))

        assertTrue(result.fileName.contains("_p5_"))
    }

    @Test
    fun invoke_producesStableNameForSameUrl() {
        val url = "https://cdn.example.com/page.png"

        assertEquals(
            builder("One", target(url, 0)),
            builder("One", target(url, 0)),
        )
    }

    @Test
    fun invoke_producesDifferentNamesForDifferentUrls() {
        assertNotEquals(
            builder("One", target("https://cdn.example.com/page-a.png", 0)).fileName,
            builder("One", target("https://cdn.example.com/page-b.png", 0)).fileName,
        )
    }

    private fun target(
        pageUrl: String,
        pageIndex: Int,
    ) = ReaderPageActionTarget(pageUrl = pageUrl, pageIndex = pageIndex)
}
