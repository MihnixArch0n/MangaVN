package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.models.toDomainModel
import com.example.mybookslibrary.domain.model.MangaModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MangaRepository(
    private val api: MangaDexApi,
    private val preferencesDataStore: UserPreferencesDataStore
) {
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


