package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.models.MangaDexConstants
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

    private suspend fun lang(): String = preferencesDataStore.getLanguage()

    fun getDiscoverManga(limit: Int = 20, offset: Int = 0): Flow<Result<List<MangaModel>>> = flow {
        val preferredLang = lang()
        val result = runCatching {
            api.getMangaList(limit = limit, offset = offset, includes = listOf("cover_art"))
                .data.map { it.toDomainModel(preferredLang) }
        }
        emit(result)
    }

    fun searchManga(query: String): Flow<Result<List<MangaModel>>> = flow {
        val preferredLang = lang()
        val result = runCatching {
            api.searchManga(title = query, includes = listOf("cover_art"))
                .data.map { it.toDomainModel(preferredLang) }
        }
        emit(result)
    }

    suspend fun getMangaDetail(mangaId: String): Result<MangaModel> = runCatching {
        val preferredLang = lang()
        api.getMangaDetail(mangaId).data.toDomainModel(preferredLang)
    }

    suspend fun getMangaFeed(
        mangaId: String,
        translatedLanguages: List<String>? = null
    ): Result<List<ChapterModel>> = runCatching {
        val languages = translatedLanguages
            ?: listOf(lang(), MangaDexConstants.LANG_EN, MangaDexConstants.LANG_VI).distinct()
        val chapters = mutableListOf<ChapterModel>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (offset < total) {
            val response = api.getMangaFeed(
                mangaId = mangaId,
                translatedLanguages = languages,
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

    suspend fun getChapterFeed(mangaId: String): Result<List<ChapterModel>> = runCatching {
        getMangaFeed(mangaId).getOrThrow()
    }

    suspend fun getChapterPages(chapterId: String): Result<List<String>> = runCatching {
        val quality = preferencesDataStore.getReaderQuality()
        val atHomeResponse = api.getAtHomeServer(chapterId)
        val baseUrl = atHomeResponse.baseUrl
        val hash = atHomeResponse.chapter.hash
        val filenames = when {
            quality == MangaDexConstants.QUALITY_DATA_SAVER && atHomeResponse.chapter.dataSaver.isNotEmpty() ->
                atHomeResponse.chapter.dataSaver
            else -> atHomeResponse.chapter.data
        }
        filenames.map { filename -> "$baseUrl/$quality/$hash/$filename" }
    }
}
