package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory index of chapters that have completed offline downloads.
 *
 * The cache loads downloaded chapter ids once from Room during app lifetime and
 * then serves O(1) reactive lookups for UI and reader code.
 */
@Singleton
class DownloadedChapterCache @Inject constructor(
    private val chapterDao: ChapterDao,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {

    private val _downloadedChapterIds = MutableStateFlow<Set<String>>(emptySet())

    val downloadedChapterIds: StateFlow<Set<String>> = _downloadedChapterIds.asStateFlow()

    init {
        applicationScope.launch {
            try {
                val downloadedIds = chapterDao.getDownloadedChapterIds().toSet()
                _downloadedChapterIds.value = downloadedIds
                Timber.d("DownloadedChapterCache initialized: count=%d", downloadedIds.size)
            } catch (t: Throwable) {
                Timber.e(t, "DownloadedChapterCache initialization failed")
            }
        }
    }

    /**
     * Observes whether [chapterId] exists in the downloaded chapter set.
     */
    fun isChapterDownloadedFlow(chapterId: String): Flow<Boolean> {
        return downloadedChapterIds.map { chapterId in it }
    }

    /**
     * Marks [chapterId] as downloaded in memory after persistence succeeds.
     */
    suspend fun markDownloaded(chapterId: String) {
        _downloadedChapterIds.update { it + chapterId }
        Timber.d("DownloadedChapterCache markDownloaded: chapterId=%s", chapterId)
    }

    /**
     * Removes [chapterId] from the downloaded set after deletion succeeds.
     */
    suspend fun markNotDownloaded(chapterId: String) {
        _downloadedChapterIds.update { it - chapterId }
        Timber.d("DownloadedChapterCache markNotDownloaded: chapterId=%s", chapterId)
    }
}
