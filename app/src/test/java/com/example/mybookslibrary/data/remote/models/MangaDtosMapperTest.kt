package com.example.mybookslibrary.data.remote.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phủ các mapper DTO -> domain trong MangaDtos.kt: chọn ngôn ngữ ưu tiên + fallback,
 * ghép cover URL, và default khi attributes thiếu. Khởi tạo DTO trực tiếp để kiểm tra
 * constructor defaults độc lập với JSON parser.
 */
class MangaDtosMapperTest {
    private fun relationship(
        type: String,
        fileName: String? = null,
    ) = RelationshipDto(
        id = "rel-$type",
        type = type,
        attributes = fileName?.let { RelationshipAttributesDto(fileName = it) },
    )

    // ---- MangaDataDto.toDomainModel ----

    @Test
    fun mangaToDomain_uuTienNgonNguYeuCau() {
        val dto =
            MangaDataDto(
                id = "m1",
                attributes =
                    MangaAttributesDto(
                        title = mapOf("en" to "English Title", "vi" to "Tựa Việt"),
                        description = mapOf("en" to "Desc EN", "vi" to "Mô tả VI"),
                        tags =
                            listOf(
                                TagDto(TagAttributesDto(name = mapOf("en" to "Action", "vi" to "Hành động"))),
                            ),
                    ),
                relationships = listOf(relationship("cover_art", "cover.jpg")),
            )

        val model = dto.toDomainModel(preferredLang = "en")

        assertEquals("m1", model.id)
        assertEquals("English Title", model.title)
        assertEquals("Desc EN", model.description)
        assertEquals(listOf("Action"), model.tags)
        assertEquals("https://uploads.mangadex.org/covers/m1/cover.jpg", model.coverArt)
    }

    @Test
    fun mangaToDomain_fallbackLangKhiThieuNgonNguYeuCau() {
        // preferredLang="vi" -> fallback="en"; title chỉ có "en" -> dùng fallback.
        val dto =
            MangaDataDto(
                id = "m2",
                attributes =
                    MangaAttributesDto(
                        title = mapOf("en" to "Only English"),
                        description = mapOf("en" to "Only EN desc"),
                        tags = listOf(TagDto(TagAttributesDto(name = mapOf("en" to "Drama")))),
                    ),
            )

        val model = dto.toDomainModel(preferredLang = "vi")

        assertEquals("Only English", model.title)
        assertEquals("Only EN desc", model.description)
        assertEquals(listOf("Drama"), model.tags)
        assertNull(model.coverArt)
    }

    @Test
    fun mangaToDomain_lastResortLayGiaTriDauTienKhiThieuCaHaiLang() {
        // title không có en/vi -> values.firstOrNull(); tag name cũng vậy.
        val dto =
            MangaDataDto(
                id = "m3",
                attributes =
                    MangaAttributesDto(
                        title = mapOf("fr" to "Titre FR"),
                        description = mapOf("fr" to "Desc FR"),
                        tags = listOf(TagDto(TagAttributesDto(name = mapOf("fr" to "Aventure")))),
                    ),
            )

        val model = dto.toDomainModel(preferredLang = "en")

        assertEquals("Titre FR", model.title)
        assertEquals("Desc FR", model.description)
        assertEquals(listOf("Aventure"), model.tags)
    }

    @Test
    fun mangaToDomain_thieuHetThiRong() {
        val dto = MangaDataDto(id = "m4", attributes = MangaAttributesDto())

        val model = dto.toDomainModel()

        assertEquals("", model.title)
        assertEquals("", model.description)
        assertEquals(emptyList<String>(), model.tags)
        assertNull(model.coverArt)
    }

    @Test
    fun mangaToDomain_tagThieuAttributesBiBoQua() {
        val dto =
            MangaDataDto(
                id = "m5",
                attributes =
                    MangaAttributesDto(
                        title = mapOf("en" to "T"),
                        tags = listOf(TagDto(attributes = null)),
                    ),
            )

        assertEquals(emptyList<String>(), dto.toDomainModel().tags)
    }

    // ---- extractCoverUrl ----

    @Test
    fun extractCoverUrl_khongCoCoverArtThiNull() {
        val dto =
            MangaDataDto(
                id = "m6",
                attributes = MangaAttributesDto(),
                relationships = listOf(relationship("author")),
            )

        assertNull(dto.extractCoverUrl())
    }

    @Test
    fun extractCoverUrl_coverArtNhungThieuFileNameThiNull() {
        val dto =
            MangaDataDto(
                id = "m7",
                attributes = MangaAttributesDto(),
                relationships = listOf(relationship("cover_art", fileName = null)),
            )

        assertNull(dto.extractCoverUrl())
    }

    // ---- TagItemDto.toDomainModel ----

    @Test
    fun tagItemToDomain_chonTheoNgonNguVaGiuGroup() {
        val dto =
            TagItemDto(
                id = "t1",
                attributes =
                    TagItemAttributesDto(
                        name = mapOf("en" to "Romance", "vi" to "Lãng mạn"),
                        group = "genre",
                    ),
            )

        val tag = dto.toDomainModel(preferredLang = "vi")

        assertEquals("t1", tag.id)
        assertEquals("Lãng mạn", tag.name)
        assertEquals("genre", tag.group)
    }

    @Test
    fun tagItemToDomain_fallbackVaLastResortVaRong() {
        // chỉ có "fr" -> last resort firstOrNull
        val onlyFr = TagItemDto(id = "t2", attributes = TagItemAttributesDto(name = mapOf("fr" to "Comédie")))
        assertEquals("Comédie", onlyFr.toDomainModel(preferredLang = "en").name)

        // không có tên -> ""
        val empty = TagItemDto(id = "t3", attributes = TagItemAttributesDto())
        assertEquals("", empty.toDomainModel().name)
    }

    // ---- ChapterDto.toDomainModel ----

    @Test
    fun chapterToDomain_layMangaIdTuRelationship() {
        val dto =
            ChapterDto(
                id = "c1",
                attributes =
                    ChapterAttributesDto(
                        volume = "1",
                        chapter = "10",
                        title = "Mở đầu",
                        pages = 20,
                        isUnavailable = false,
                    ),
                relationships = listOf(relationship("manga").copy(id = "manga-real")),
            )

        val model = dto.toDomainModel(fallbackMangaId = "fallback")

        assertEquals("c1", model.id)
        assertEquals("manga-real", model.mangaId)
        assertEquals("1", model.volume)
        assertEquals("10", model.chapterNumber)
        assertEquals("Mở đầu", model.title)
        assertEquals(20, model.pages)
        assertEquals(false, model.isUnavailable)
    }

    @Test
    fun chapterToDomain_khongCoRelationshipManga_dungFallback() {
        val dto = ChapterDto(id = "c2", attributes = null, relationships = emptyList())

        val model = dto.toDomainModel(fallbackMangaId = "fallback-id")

        assertEquals("fallback-id", model.mangaId)
        // attributes null -> default
        assertNull(model.volume)
        assertNull(model.chapterNumber)
        assertNull(model.title)
        assertEquals(0, model.pages)
        assertEquals(false, model.isUnavailable)
    }

    @Test
    fun dtoDataClass_giaTriMacDinhDung() {
        // Construct BỎ arg optional -> chạy nhánh giá trị mặc định (= null / = emptyMap()).
        // Constructor defaults cũng là hợp đồng khi kotlinx.serialization gặp field bị thiếu.
        assertNull(RelationshipDto(id = "r", type = "t").attributes)
        assertNull(RelationshipAttributesDto().fileName)
        assertNull(TagDto().attributes)
        assertTrue(TagAttributesDto().name.isEmpty())
        assertEquals("", TagItemDto(id = "t").attributes.group)
        assertNull(AtHomeResponseDto().result)
    }

    @Test
    fun chapterToDomain_isUnavailableTrue() {
        val dto =
            ChapterDto(
                id = "c3",
                attributes = ChapterAttributesDto(isUnavailable = true, pages = 5),
            )

        val model = dto.toDomainModel(fallbackMangaId = "f")

        assertEquals(true, model.isUnavailable)
        assertEquals(5, model.pages)
    }
}
