package com.example.mybookslibrary.data.remote

import com.example.mybookslibrary.data.remote.models.AtHomeResponseDto
import com.example.mybookslibrary.data.remote.models.ChapterListDto
import com.example.mybookslibrary.data.remote.models.MangaListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexApi {

    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaListResponseDto

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") mangaId: String,
        @Query("translatedLanguage[]") translatedLanguages: List<String> = listOf("en", "vi"),
        @Query("order[volume]") volumeOrder: String = "asc",
        @Query("order[chapter]") chapterOrder: String = "asc",
        @Query("limit") limit: Int = 500,
        @Query("offset") offset: Int = 0,
        @Query("includeUnavailable") includeUnavailable: Int = 0
    ): Response<ChapterListDto>

    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String
    ): AtHomeResponseDto
}



