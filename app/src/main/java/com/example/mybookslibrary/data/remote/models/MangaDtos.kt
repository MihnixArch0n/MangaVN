package com.example.mybookslibrary.data.remote.models

import com.example.mybookslibrary.domain.model.MangaModel
import com.google.gson.annotations.SerializedName

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

fun MangaDataDto.toDomainModel(): MangaModel {
    val mainTitle = attributes.title["en"]
        ?: attributes.title["vi"]
        ?: attributes.title.values.firstOrNull()
        ?: "Untitled"

    val mainDescription = attributes.description["en"]
        ?: attributes.description["vi"]
        ?: attributes.description.values.firstOrNull()
        ?: ""

    val genres = attributes.tags.mapNotNull { tag ->
        tag.attributes?.name?.get("en")
            ?: tag.attributes?.name?.values?.firstOrNull()
    }

    return MangaModel(
        id = id,
        title = mainTitle,
        description = mainDescription,
        coverArt = extractCoverUrl(),
        rating = null,
        tags = genres
    )
}

/**
 * MangaDex trả cover qua relationships có type = cover_art.
 * URL chuẩn: https://uploads.mangadex.org/covers/{manga_id}/{file_name}
 */
fun MangaDataDto.extractCoverUrl(): String? {
    val coverFileName = relationships
        .firstOrNull { it.type == "cover_art" }
        ?.attributes
        ?.fileName
        ?: return null

    return "https://uploads.mangadex.org/covers/$id/$coverFileName"
}

// At-Home Server DTOs for Reader
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

