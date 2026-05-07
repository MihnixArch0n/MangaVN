package com.example.mybookslibrary.data.remote

import com.example.mybookslibrary.data.remote.models.AtHomeResponseDto
import com.example.mybookslibrary.data.remote.models.ChapterListDto
import com.example.mybookslibrary.data.remote.models.MangaDetailResponseDto
import com.example.mybookslibrary.data.remote.models.MangaListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit interface cho MangaDex REST API (base URL: https://api.mangadex.org/)
interface MangaDexApi {

    // Lấy danh sách manga cho trang Discover, bao gồm cover art qua includes[]
    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaListResponseDto

    // Tìm kiếm manga theo tên, trả về kết quả với cover art
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 20,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaListResponseDto

    // Lấy chi tiết 1 manga cụ thể (dùng khi vào Detail từ Library)
    @GET("manga/{mangaId}")
    suspend fun getMangaDetail(
        @Path("mangaId") mangaId: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art")
    ): MangaDetailResponseDto

    // Lấy danh sách chapter của manga, hỗ trợ phân trang và loại chapter unavailable
    @GET("manga/{mangaId}/feed")
    suspend fun getMangaFeed(
        @Path("mangaId") mangaId: String,
        @Query("translatedLanguage[]") translatedLanguages: List<String> = listOf("en", "vi"),
        @Query("order[volume]") volumeOrder: String = "asc",
        @Query("order[chapter]") chapterOrder: String = "asc",
        @Query("limit") limit: Int = 500,
        @Query("offset") offset: Int = 0,
        @Query("includeUnavailable") includeUnavailable: Int = 0
    ): Response<ChapterListDto>

    // At-Home API: lấy URL server + danh sách file ảnh của 1 chapter để đọc
    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String
    ): AtHomeResponseDto
}
