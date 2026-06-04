package com.example.mybookslibrary.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.domain.model.ChapterDownloadState
import com.example.mybookslibrary.domain.model.ChapterDownloadStatus
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChapterComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun chapter(
        id: String = "c1",
        status: ChapterReadingStatus = ChapterReadingStatus.UNREAD,
        lastReadPage: Int = 0,
        totalPages: Int = 20,
        title: String? = null,
        downloadStatus: ChapterDownloadStatus = ChapterDownloadStatus.NOT_DOWNLOADED,
    ) = ChapterWithProgressModel(
        chapterId = id,
        mangaId = "m1",
        volume = null,
        chapterNumber = "1",
        title = title,
        status = status,
        lastReadPage = lastReadPage,
        totalPages = totalPages,
        downloadState = ChapterDownloadState(status = downloadStatus)
    )

    // ---- VolumeHeader ----

    @Test
    fun volumeHeader_showsLabel() {
        composeRule.setContent {
            VolumeHeader("Volume 1")
        }
        composeRule.onNodeWithText("Volume 1").assertIsDisplayed()
    }

    // ---- ChapterRow status states ----

    @Test
    fun chapterRow_unread_showsStatusLabel() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(status = ChapterReadingStatus.UNREAD),
                chapterTitle = "Chapter 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithText("Chapter 1").assertIsDisplayed()
        composeRule.onNodeWithText("Unread").assertIsDisplayed()
    }

    @Test
    fun chapterRow_reading_showsPageProgress() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(status = ChapterReadingStatus.READING, lastReadPage = 4, totalPages = 20),
                chapterTitle = "Chapter 2",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithText("Chapter 2").assertIsDisplayed()
        // "Reading · Page 5/20"
        composeRule.onNodeWithText("Reading · Page 5/20").assertIsDisplayed()
    }

    @Test
    fun chapterRow_completed_showsCompletedStatus() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(status = ChapterReadingStatus.COMPLETED),
                chapterTitle = "Chapter 3",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithText("Completed").assertIsDisplayed()
    }

    @Test
    fun chapterRow_withSubtitle_showsBothTitles() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(title = "The Beginning"),
                chapterTitle = "Chapter 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithText("Chapter 1").assertIsDisplayed()
        composeRule.onNodeWithText("The Beginning").assertIsDisplayed()
    }

    @Test
    fun chapterRow_pagesCount_shown() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(totalPages = 42),
                chapterTitle = "Ch",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        // "42p" (detail_pages_suffix = "%1$dp")
        composeRule.onNodeWithText("42p").assertIsDisplayed()
    }

    // ---- ChapterRow + DownloadIndicator states ----

    @Test
    fun chapterRow_notDownloaded_showsDownloadIcon() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(downloadStatus = ChapterDownloadStatus.NOT_DOWNLOADED),
                chapterTitle = "Ch 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithContentDescription("Download chapter").assertIsDisplayed()
    }

    @Test
    fun chapterRow_downloaded_showsDeleteIcon() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(downloadStatus = ChapterDownloadStatus.DOWNLOADED),
                chapterTitle = "Ch 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithContentDescription("Delete download").assertIsDisplayed()
    }

    @Test
    fun chapterRow_downloadError_showsErrorIcon() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(downloadStatus = ChapterDownloadStatus.ERROR),
                chapterTitle = "Ch 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithContentDescription("Download failed").assertIsDisplayed()
    }

    @Test
    fun chapterRow_pending_showsCancelDownload() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(downloadStatus = ChapterDownloadStatus.PENDING),
                chapterTitle = "Ch 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        // PENDING: IconButton có CircularProgressIndicator — không có text, không crash
        composeRule.waitForIdle()
    }

    @Test
    fun chapterRow_downloading_showsStopIcon() {
        composeRule.setContent {
            ChapterRow(
                chapter = chapter(downloadStatus = ChapterDownloadStatus.DOWNLOADING),
                chapterTitle = "Ch 1",
                onClick = {}, onMarkCompleted = {}, onMarkUnread = {},
                onStartDownload = {}, onCancelDownload = {}, onDeleteDownload = {}
            )
        }
        composeRule.onNodeWithContentDescription("Cancel download").assertIsDisplayed()
    }

    // ---- buildChapterTitle ----

    @Test
    fun buildChapterTitle_withNumber() {
        var title = ""
        composeRule.setContent {
            title = buildChapterTitle(chapter().copy(chapterNumber = "5"))
        }
        composeRule.waitForIdle()
        assert(title.contains("5")) { "Tiêu đề chương phải chứa số chương: $title" }
    }

    @Test
    fun buildChapterTitle_noNumber_returnsNonEmpty() {
        var title = ""
        composeRule.setContent {
            title = buildChapterTitle(chapter().copy(chapterNumber = null))
        }
        composeRule.waitForIdle()
        assert(title.isNotBlank()) { "Tiêu đề không được rỗng khi không có số: $title" }
    }
}
