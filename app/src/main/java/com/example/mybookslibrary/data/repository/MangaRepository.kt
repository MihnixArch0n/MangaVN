package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.models.MangaDexConstants
import com.example.mybookslibrary.data.remote.models.toDomainModel
import com.example.mybookslibrary.domain.model.ChapterModel
import com.example.mybookslibrary.domain.model.MangaModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MangaRepository(
    private val api: MangaDexApi,
    private val preferencesDataStore: UserPreferencesDataStore
) {
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

    suspend fun getChapterFeed(mangaId: String): Result<List<ChapterModel>> = runCatching {
        api.getChapterFeed(mangaId = mangaId).data.map { dto ->
            ChapterModel(
                id = dto.id,
                chapter = dto.attributes.chapter,
                title = dto.attributes.title,
                pages = dto.attributes.pages,
                volume = dto.attributes.volume
            )
        }
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
