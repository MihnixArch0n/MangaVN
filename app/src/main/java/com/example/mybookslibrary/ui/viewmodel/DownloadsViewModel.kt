package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Một chapter đã tải trên màn Downloads.
 * [mangaTitle] null = manga không còn trong library; [chapterLabel] null = không còn
 * metadata (UI hiện "Chapter không xác định") — vẫn xóa được.
 */
data class DownloadedChapterUi(
    val chapterId: String,
    val mangaId: String,
    val mangaTitle: String?,
    val chapterLabel: String?,
    val sizeBytes: Long,
)

/** [chapters] null = đang scan filesystem (loading) — tránh flash EmptyState. */
data class DownloadsUiState(
    val chapters: List<DownloadedChapterUi>? = null,
    val totalSizeBytes: Long = 0L,
)

/** Sự kiện one-shot cho snackbar. */
enum class DownloadsEvent { DELETE_FAILED }

@HiltViewModel
class DownloadsViewModel
    @Inject
    constructor(
        chapterDao: ChapterDao,
        downloadedChapterCache: DownloadedChapterCache,
        storage: OfflineDownloadStorage,
        private val downloadManager: OfflineDownloadManager,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        // extraBufferCapacity=1: emit không treo khi collector chưa kịp attach (pattern ReaderViewModel)
        private val _events = MutableSharedFlow<DownloadsEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<DownloadsEvent> = _events

        // Filesystem là nguồn sự thật về "đã tải" (cache emit khi add/remove); Room chỉ
        // map id -> title/volume/chapter để hiển thị. Chapter mất metadata vẫn phải hiện.
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<DownloadsUiState> =
            downloadedChapterCache.downloadedChapterIds
                .mapLatest { _ ->
                    val dirInfos = storage.listDownloadedChapters()
                    // Guard rỗng: không gọi DAO với IN () (hành vi Room với collection rỗng không được document)
                    val metadataById =
                        if (dirInfos.isEmpty()) {
                            emptyMap()
                        } else {
                            chapterDao
                                .getDownloadedChapterInfo(dirInfos.mapTo(mutableSetOf()) { it.chapterId })
                                .associateBy { it.chapterId }
                        }
                    val chapters =
                        dirInfos
                            .map { dir ->
                                val metadata = metadataById[dir.chapterId]
                                DownloadedChapterUi(
                                    chapterId = dir.chapterId,
                                    mangaId = dir.mangaId,
                                    mangaTitle = metadata?.mangaTitle,
                                    chapterLabel = metadata?.buildChapterLabel(),
                                    sizeBytes = dir.sizeBytes,
                                )
                            }.sortedWith(
                                // Có title trước (theo tên + số chương), mất title xuống cuối
                                compareBy(
                                    { it.mangaTitle == null },
                                    { it.mangaTitle },
                                    { chapter ->
                                        chapter.chapterLabel
                                            ?.let { label -> numericChapterOrder(label) }
                                            ?: Double.MAX_VALUE
                                    },
                                ),
                            )
                    DownloadsUiState(
                        chapters = chapters,
                        totalSizeBytes = chapters.sumOf { it.sizeBytes },
                    )
                }.flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DownloadsUiState())

        fun deleteDownload(mangaId: String, chapterId: String) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    downloadManager.deleteDownload(mangaId, chapterId)
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    Timber.e(e, "deleteDownload thất bại: mangaId=%s chapterId=%s", mangaId, chapterId)
                    _events.emit(DownloadsEvent.DELETE_FAILED)
                }
            }
        }

        companion object {
            private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

            /** "Vol. 2 Ch. 10.5" → 10.5 để sort theo số thay vì alphabet. */
            private fun numericChapterOrder(label: String): Double =
                label.substringAfter("Ch. ", "").toDoubleOrNull() ?: Double.MAX_VALUE
        }
    }
