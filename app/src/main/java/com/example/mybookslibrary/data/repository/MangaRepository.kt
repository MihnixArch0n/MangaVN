package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.models.toDomainModel
import com.example.mybookslibrary.data.remote.models.toDomainModel as chapterToDomainModel
import com.example.mybookslibrary.domain.model.ChapterModel
import com.example.mybookslibrary.domain.model.MangaModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class MangaRepository(
    private val api: MangaDexApi,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    companion object {
        private const val FEED_PAGE_LIMIT = 500
    }

    fun getDiscoverManga(limit: Int = 20, offset: Int = 0): Flow<Result<List<MangaModel>>> = flow {
        val result = runCatching {
            api.getMangaList(
                limit = limit,
                offset = offset,
                includes = listOf("cover_art")
            ).data.map { it.toDomainModel() }
        }
        emit(result)
    }

    suspend fun getMangaFeed(
        mangaId: String,
        translatedLanguages: List<String> = listOf("en", "vi")
    ): Result<List<ChapterModel>> = runCatching {
        val chapters = mutableListOf<ChapterModel>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (offset < total) {
            val response = api.getMangaFeed(
                mangaId = mangaId,
                translatedLanguages = translatedLanguages,
                limit = FEED_PAGE_LIMIT,
                offset = offset,
                includeUnavailable = 0
            )

            if (!response.isSuccessful) {
                throw IOException("Manga feed request failed: HTTP ${response.code()}")
            }

            val body = response.body() ?: throw IOException("Manga feed response body is null")
            chapters += body.data
                .asSequence()
                .map { it.chapterToDomainModel(mangaId) }
                .filterNot { it.isUnavailable }
                .toList()

            total = body.total
            val pageSize = body.data.size
            if (pageSize == 0) break
            offset += pageSize
        }

        chapters
    }

    suspend fun getChapterPages(chapterId: String): Result<List<String>> = runCatching {
        // Step 1: Fetch user's preferred quality
        val quality = preferencesDataStore.getReaderQuality()

        // Step 2: Call At-Home API
        val atHomeResponse = api.getAtHomeServer(chapterId)

        // Step 3: Extract baseUrl, hash, and filenames
        val baseUrl = atHomeResponse.baseUrl
        val hash = atHomeResponse.chapter.hash
        val filenames = when {
            quality == "data-saver" && atHomeResponse.chapter.dataSaver.isNotEmpty() ->
                atHomeResponse.chapter.dataSaver
            else -> atHomeResponse.chapter.data
        }

        // Step 4: Build full URLs
        filenames.map { filename ->
            "$baseUrl/$quality/$hash/$filename"
        }
    }
}


