package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.models.AtHomeReportRequest
import com.example.mybookslibrary.data.remote.models.MangaDexConstants
import com.example.mybookslibrary.data.remote.models.toDomainModel
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.domain.model.ChapterModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.model.MangaTag
import com.example.mybookslibrary.domain.model.SearchFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import com.example.mybookslibrary.data.remote.models.toDomainModel as chapterToDomainModel

class MangaRepository(
    private val api: MangaDexApi,
    private val preferencesDataStore: UserPreferencesDataStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val FEED_PAGE_LIMIT = 500
    }

    @Volatile
    private var cachedTags: List<MangaTag>? = null
    private val tagsMutex = Mutex()

    private suspend fun lang(): String = preferencesDataStore.getLanguage()

    /** Flow emit danh sách manga cho màn hình Discover. Emit một lần rồi complete. */
    fun getDiscoverManga(
        limit: Int = 20,
        offset: Int = 0,
    ): Flow<Result<List<MangaModel>>> =
        flow {
            val preferredLang = lang()
            val result =
                runCatching {
                    api
                        .getMangaList(limit = limit, offset = offset, includes = listOf("cover_art"))
                        .data
                        .map { it.toDomainModel(preferredLang) }
                }
            emit(result)
        }.flowOn(ioDispatcher)

    /** Flow emit kết quả tìm kiếm manga với [query] và [filters]. Emit một lần rồi complete. */
    fun searchManga(
        query: String,
        filters: SearchFilters = SearchFilters(),
    ): Flow<Result<List<MangaModel>>> =
        flow {
            val preferredLang = lang()
            val result =
                runCatching {
                    api
                        .searchManga(
                            title = query,
                            includes = listOf("cover_art"),
                            includedTags = filters.includedTagIds,
                            translatedLanguages = filters.languages,
                            contentRatings = filters.contentRatings,
                            statuses = filters.statuses,
                        ).data
                        .map { it.toDomainModel(preferredLang) }
                }
            emit(result)
        }.flowOn(ioDispatcher)

    /**
     * Tải danh sách tag MangaDex (genre/theme/format/content) cho bộ lọc Search.
     * Cache in-memory vì tag hiếm khi đổi; chỉ cache khi thành công.
     */
    suspend fun getTags(): Result<List<MangaTag>> {
        cachedTags?.let { return Result.success(it) }
        return tagsMutex.withLock {
            cachedTags?.let { return@withLock Result.success(it) }
            runCatching {
                withContext(ioDispatcher) {
                    val preferredLang = lang()
                    api.getTags().data.map { it.toDomainModel(preferredLang) }
                }
            }.onSuccess { cachedTags = it }
        }
    }

    /** Lấy thông tin chi tiết của một manga. */
    suspend fun getMangaDetail(mangaId: String): Result<MangaModel> =
        runCatching {
            val preferredLang = lang()
            api.getMangaDetail(mangaId).data.toDomainModel(preferredLang)
        }

    /**
     * Tải toàn bộ danh sách chapter của manga, tự động paginate (500 chapters/trang).
     * Lọc bỏ chapter `isUnavailable`. Ngôn ngữ mặc định: preference + EN + VI.
     *
     * @param translatedLanguages Override ngôn ngữ; `null` = dùng preference người dùng.
     */
    suspend fun getMangaFeed(
        mangaId: String,
        translatedLanguages: List<String>? = null,
    ): Result<List<ChapterModel>> =
        runCatching {
            val languages =
                translatedLanguages
                    ?: listOf(lang(), MangaDexConstants.LANG_EN, MangaDexConstants.LANG_VI).distinct()
            val chapters = mutableListOf<ChapterModel>()
            var offset = 0
            var total = Int.MAX_VALUE

            while (offset < total) {
                val response =
                    api.getMangaFeed(
                        mangaId = mangaId,
                        translatedLanguages = languages,
                        limit = FEED_PAGE_LIMIT,
                        offset = offset,
                        includeUnavailable = 0,
                    )

                if (!response.isSuccessful) {
                    throw IOException("Manga feed request failed: HTTP ${response.code()}")
                }

                val body = response.body() ?: throw IOException("Manga feed response body is null")
                chapters +=
                    body.data
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

    /** Alias cho [getMangaFeed] với ngôn ngữ mặc định. Dùng cho offline download. */
    suspend fun getChapterFeed(mangaId: String): Result<List<ChapterModel>> =
        runCatching {
            getMangaFeed(mangaId).getOrThrow()
        }

    /** Lấy danh sách URL ảnh của chapter. Shortcut cho [getChapterDelivery] + [ChapterDelivery.pageUrls]. */
    suspend fun getChapterPages(chapterId: String): Result<List<String>> =
        runCatching {
            getChapterDelivery(chapterId).getOrThrow().pageUrls()
        }

    /**
     * Lấy thông tin delivery từ MangaDex@Home server cho chapter.
     *
     * Validate response envelope (HTTP 200 nhưng `result != "ok"` hoặc thiếu field)
     * → throw [IllegalStateException] thay vì crash với NPE ở reader.
     * Chọn `dataSaver` hay `data` filenames dựa trên quality preference.
     */
    suspend fun getChapterDelivery(chapterId: String): Result<ChapterDelivery> =
        runCatching {
            val quality = preferencesDataStore.getReaderQuality()
            val atHomeResponse = api.getAtHomeServer(chapterId)

            // Validate error-envelope (HTTP 200 nhưng thiếu baseUrl/chapter) trước khi build URL,
            // tránh NullPointerException khó debug ở reader.
            if (atHomeResponse.result != "ok") {
                throw IllegalStateException(
                    "Phản hồi At-Home không hợp lệ cho chapter $chapterId (result=${atHomeResponse.result})",
                )
            }
            val baseUrl =
                atHomeResponse.baseUrl
                    ?: throw IllegalStateException("Phản hồi At-Home thiếu baseUrl cho chapter $chapterId")
            val chapter =
                atHomeResponse.chapter
                    ?: throw IllegalStateException("Phản hồi At-Home thiếu chapter cho chapter $chapterId")
            val hash =
                chapter.hash
                    ?: throw IllegalStateException("Phản hồi At-Home thiếu hash cho chapter $chapterId")

            val filenames =
                when {
                    quality == MangaDexConstants.QUALITY_DATA_SAVER && chapter.dataSaver.isNotEmpty() ->
                        chapter.dataSaver
                    else -> chapter.data
                }

            ChapterDelivery(
                baseUrl = baseUrl,
                quality = quality,
                hash = hash,
                filenames = filenames,
            )
        }

    /**
     * Gửi telemetry report về MangaDex@Home node. **Nuốt lỗi mạng silently** —
     * report thất bại không ảnh hưởng trải nghiệm đọc. Chỉ re-throw [CancellationException].
     */
    suspend fun sendAtHomeReport(request: AtHomeReportRequest) {
        withContext(ioDispatcher) {
            try {
                Timber.d("sendAtHomeReport start: payload=%s", request)
                val response = api.sendAtHomeReport(request)
                Timber.d(
                    "sendAtHomeReport end: url=%s success=%s http=%d",
                    request.url,
                    request.success,
                    response.code(),
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (t: Throwable) {
                Timber.e(t, "sendAtHomeReport failed silently: url=%s", request.url)
            }
        }
    }
}

data class ChapterDelivery(
    val baseUrl: String,
    val quality: String,
    val hash: String,
    val filenames: List<String>,
) {
    /**
     * Build URL cho trang [pageIndex].
     * @throws IllegalArgumentException nếu [pageIndex] ngoài phạm vi.
     */
    fun pageUrl(pageIndex: Int): String {
        val name =
            filenames.getOrNull(pageIndex)
                ?: throw IllegalArgumentException(
                    "pageIndex $pageIndex ngoài phạm vi (chapter có ${filenames.size} trang)",
                )
        return "$baseUrl/$quality/$hash/$name"
    }

    /** Build URL cho tất cả trang của chapter theo thứ tự. */
    fun pageUrls(): List<String> = filenames.indices.map(::pageUrl)
}
