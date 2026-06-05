package com.example.mybookslibrary.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test property getter `chapter` của [ChapterModel].
 */
class ChapterModelTest {
    @Test
    fun chapter_returnsChapterNumber() {
        val model =
            ChapterModel(
                id = "c1",
                mangaId = "m1",
                volume = null,
                chapterNumber = "42",
                title = null,
                pages = 20,
                isUnavailable = false,
            )
        assertEquals("42", model.chapter)
    }

    @Test
    fun chapter_whenChapterNumberNull_returnsNull() {
        val model =
            ChapterModel(
                id = "c2",
                mangaId = "m1",
                volume = null,
                chapterNumber = null,
                title = null,
                pages = 0,
                isUnavailable = true,
            )
        assertNull(model.chapter)
    }
}
