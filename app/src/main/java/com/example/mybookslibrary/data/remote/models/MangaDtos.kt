package com.example.mybookslibrary.data.remote.models

import com.example.mybookslibrary.domain.model.ChapterModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.model.MangaTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object MangaDexConstants {
    const val QUALITY_ORIGINAL = "data"
    const val QUALITY_DATA_SAVER = "data-saver"
    const val COVER_BASE_URL = "https://uploads.mangadex.org/covers"
    const val LANG_EN = "en"
    const val LANG_VI = "vi"
    const val LANG_JA = "ja"

    // Nhóm tag trả về từ /manga/tag (attributes.group)
    const val TAG_GROUP_GENRE = "genre"
    const val TAG_GROUP_THEME = "theme"

    // Content rating hợp lệ của MangaDex
    const val RATING_SAFE = "safe"
    const val RATING_SUGGESTIVE = "suggestive"
    const val RATING_EROTICA = "erotica"

    // Trạng thái phát hành
    const val STATUS_ONGOING = "ongoing"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_HIATUS = "hiatus"
    const val STATUS_CANCELLED = "cancelled"
}

@Serializable
data class MangaListResponseDto(
    @SerialName("data") val data: List<MangaDataDto> = emptyList(),
)

@Serializable
data class MangaDataDto(
    @SerialName("id") val id: String = "",
    @SerialName("attributes") val attributes: MangaAttributesDto = MangaAttributesDto(),
    @SerialName("relationships") val relationships: List<RelationshipDto> = emptyList(),
)

@Serializable
data class MangaAttributesDto(
    @SerialName("title") val title: Map<String, String> = emptyMap(),
    @SerialName("description") val description: Map<String, String> = emptyMap(),
    @SerialName("contentRating") val contentRating: String? = null,
    @SerialName("tags") val tags: List<TagDto> = emptyList(),
)

@Serializable
data class TagDto(
    @SerialName("attributes") val attributes: TagAttributesDto? = null,
)

@Serializable
data class TagAttributesDto(
    @SerialName("name") val name: Map<String, String> = emptyMap(),
)

@Serializable
data class RelationshipDto(
    @SerialName("id") val id: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("attributes") val attributes: RelationshipAttributesDto? = null,
)

@Serializable
data class RelationshipAttributesDto(
    @SerialName("fileName") val fileName: String? = null,
)

fun MangaDataDto.toDomainModel(preferredLang: String = MangaDexConstants.LANG_EN): MangaModel {
    val fallbackLang = if (preferredLang == MangaDexConstants.LANG_VI) MangaDexConstants.LANG_EN else MangaDexConstants.LANG_VI

    val mainTitle =
        attributes.title[preferredLang]
            ?: attributes.title[fallbackLang]
            ?: attributes.title.values.firstOrNull()
            ?: ""

    val mainDescription =
        attributes.description[preferredLang]
            ?: attributes.description[fallbackLang]
            ?: attributes.description.values.firstOrNull()
            ?: ""

    val genres =
        attributes.tags.mapNotNull { tag ->
            tag.attributes?.name?.get(preferredLang)
                ?: tag.attributes?.name?.get(fallbackLang)
                ?: tag.attributes
                    ?.name
                    ?.values
                    ?.firstOrNull()
        }

    return MangaModel(
        id = id,
        title = mainTitle,
        description = mainDescription,
        coverArt = extractCoverUrl(),
        tags = genres,
    )
}

// Ghép URL cover từ relationships type=cover_art: uploads.mangadex.org/covers/{mangaId}/{fileName}
fun MangaDataDto.extractCoverUrl(): String? {
    val coverFileName =
        relationships
            .firstOrNull { it.type == "cover_art" }
            ?.attributes
            ?.fileName
            ?: return null

    return "${MangaDexConstants.COVER_BASE_URL}/$id/$coverFileName"
}

// Response của /manga/tag — danh sách tag để dựng bộ lọc Search
@Serializable
data class TagListResponseDto(
    @SerialName("data") val data: List<TagItemDto> = emptyList(),
)

@Serializable
data class TagItemDto(
    @SerialName("id") val id: String = "",
    @SerialName("attributes") val attributes: TagItemAttributesDto = TagItemAttributesDto(),
)

@Serializable
data class TagItemAttributesDto(
    @SerialName("name") val name: Map<String, String> = emptyMap(),
    @SerialName("group") val group: String = "",
)

fun TagItemDto.toDomainModel(preferredLang: String = MangaDexConstants.LANG_EN): MangaTag {
    val fallbackLang =
        if (preferredLang == MangaDexConstants.LANG_VI) MangaDexConstants.LANG_EN else MangaDexConstants.LANG_VI
    val displayName =
        attributes.name[preferredLang]
            ?: attributes.name[fallbackLang]
            ?: attributes.name.values.firstOrNull()
            ?: ""
    return MangaTag(id = id, name = displayName, group = attributes.group)
}

@Serializable
data class ChapterListDto(
    @SerialName("data") val data: List<ChapterDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("limit") val limit: Int = 0,
    @SerialName("offset") val offset: Int = 0,
)

@Serializable
data class ChapterDto(
    @SerialName("id") val id: String = "",
    @SerialName("attributes") val attributes: ChapterAttributesDto? = null,
    @SerialName("relationships") val relationships: List<RelationshipDto> = emptyList(),
)

// Response chi tiết 1 manga
@Serializable
data class MangaDetailResponseDto(
    @SerialName("data") val data: MangaDataDto = MangaDataDto(),
)

@Serializable
data class ChapterAttributesDto(
    @SerialName("volume") val volume: String? = null,
    @SerialName("chapter") val chapter: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("translatedLanguage") val translatedLanguage: String? = null,
    @SerialName("pages") val pages: Int? = null,
    @SerialName("isUnavailable") val isUnavailable: Boolean? = null,
)

fun ChapterDto.toDomainModel(fallbackMangaId: String): ChapterModel {
    val mangaId =
        relationships
            .firstOrNull { it.type == "manga" }
            ?.id
            ?: fallbackMangaId

    return ChapterModel(
        id = id,
        mangaId = mangaId,
        volume = attributes?.volume,
        chapterNumber = attributes?.chapter,
        title = attributes?.title,
        pages = attributes?.pages ?: 0,
        isUnavailable = attributes?.isUnavailable == true,
    )
}

// At-Home Server DTOs for Reader
@Serializable
data class AtHomeResponseDto(
    // Nullable + default: MangaDex có thể trả error-envelope (HTTP 200) thiếu các field này;
    // validate ở getChapterDelivery trước khi dựng URL.
    @SerialName("result") val result: String? = null,
    @SerialName("baseUrl") val baseUrl: String? = null,
    @SerialName("chapter") val chapter: AtHomeChapterDto? = null,
)

@Serializable
data class AtHomeChapterDto(
    @SerialName("hash") val hash: String? = null,
    @SerialName("data") val data: List<String> = emptyList(),
    @SerialName("dataSaver") val dataSaver: List<String> = emptyList(),
)

@Serializable
data class AtHomeReportRequest(
    @SerialName("url") val url: String,
    @SerialName("success") val success: Boolean,
    @SerialName("bytes") val bytes: Int,
    @SerialName("duration") val duration: Long,
    @SerialName("cached") val cached: Boolean,
)
