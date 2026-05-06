package com.example.mybookslibrary.data.remote.models

import com.example.mybookslibrary.domain.model.MangaModel
import com.google.gson.annotations.SerializedName

object MangaDexConstants {
    const val QUALITY_ORIGINAL = "data"
    const val QUALITY_DATA_SAVER = "data-saver"
    const val COVER_BASE_URL = "https://uploads.mangadex.org/covers"
    const val LANG_EN = "en"
    const val LANG_VI = "vi"
}

data class MangaListResponseDto(
    @SerializedName("data") val data: List<MangaDataDto> = emptyList()
)

data class MangaDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("attributes") val attributes: MangaAttributesDto,
    @SerializedName("relationships") val relationships: List<RelationshipDto> = emptyList()
)

data class MangaAttributesDto(
    @SerializedName("title") val title: Map<String, String> = emptyMap(),
    @SerializedName("description") val description: Map<String, String> = emptyMap(),
    @SerializedName("contentRating") val contentRating: String? = null,
    @SerializedName("tags") val tags: List<TagDto> = emptyList()
)

data class TagDto(
    @SerializedName("attributes") val attributes: TagAttributesDto? = null
)

data class TagAttributesDto(
    @SerializedName("name") val name: Map<String, String> = emptyMap()
)

data class RelationshipDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("attributes") val attributes: RelationshipAttributesDto? = null
)

data class RelationshipAttributesDto(
    @SerializedName("fileName") val fileName: String? = null
)

fun MangaDataDto.toDomainModel(preferredLang: String = MangaDexConstants.LANG_EN): MangaModel {
    val fallbackLang = if (preferredLang == MangaDexConstants.LANG_VI) MangaDexConstants.LANG_EN else MangaDexConstants.LANG_VI

    val mainTitle = attributes.title[preferredLang]
        ?: attributes.title[fallbackLang]
        ?: attributes.title.values.firstOrNull()
        ?: ""

    val mainDescription = attributes.description[preferredLang]
        ?: attributes.description[fallbackLang]
        ?: attributes.description.values.firstOrNull()
        ?: ""

    val genres = attributes.tags.mapNotNull { tag ->
        tag.attributes?.name?.get(preferredLang)
            ?: tag.attributes?.name?.get(fallbackLang)
            ?: tag.attributes?.name?.values?.firstOrNull()
    }

    return MangaModel(
        id = id,
        title = mainTitle,
        description = mainDescription,
        coverArt = extractCoverUrl(),
        tags = genres
    )
}

// Ghép URL cover từ relationships type=cover_art: uploads.mangadex.org/covers/{mangaId}/{fileName}
fun MangaDataDto.extractCoverUrl(): String? {
    val coverFileName = relationships
        .firstOrNull { it.type == "cover_art" }
        ?.attributes
        ?.fileName
        ?: return null

    return "${MangaDexConstants.COVER_BASE_URL}/$id/$coverFileName"
}

// Response chi tiết 1 manga
data class MangaDetailResponseDto(
    @SerializedName("data") val data: MangaDataDto
)

// DTO danh sách chapter
data class ChapterListResponseDto(
    @SerializedName("data") val data: List<ChapterDataDto> = emptyList()
)

data class ChapterDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("attributes") val attributes: ChapterAttributesDto
)

data class ChapterAttributesDto(
    @SerializedName("volume") val volume: String? = null,
    @SerializedName("chapter") val chapter: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("translatedLanguage") val translatedLanguage: String? = null,
    @SerializedName("pages") val pages: Int = 0
)

// DTO At-Home Server cho Reader (tải ảnh trang truyện)
data class AtHomeResponseDto(
    @SerializedName("result") val result: String,
    @SerializedName("baseUrl") val baseUrl: String,
    @SerializedName("chapter") val chapter: AtHomeChapterDto
)

data class AtHomeChapterDto(
    @SerializedName("hash") val hash: String,
    @SerializedName("data") val data: List<String> = emptyList(),
    @SerializedName("dataSaver") val dataSaver: List<String> = emptyList()
)

