package com.example.mybookslibrary.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.math.min

/**
 * Shared MangaDex@Home report rules used by online Coil loads and offline downloads.
 *
 * MangaDex only wants @Home reports for chapter image URLs served outside
 * `mangadex.org`; direct `uploads.mangadex.org` images are skipped.
 */
object AtHomeReportPolicy {
    const val SKIP_REPORT_HEADER = "X-MyBooksLibrary-Skip-AtHome-Report"

    /**
     * Trả về `true` nếu [rawUrl] là URL ảnh chapter cần báo cáo về MangaDex@Home.
     * URL không parse được → `false`.
     */
    fun isReportableImageUrl(rawUrl: String): Boolean = rawUrl.toHttpUrlOrNull()?.let(::isReportableImageUrl) == true

    /**
     * Trả về `true` nếu [url] cần được report về MangaDex@Home server.
     *
     * Skip `mangadex.org` (CDN chính thức). Chỉ report URL có path segment
     * `data` hoặc `data-saver` — dấu hiệu của At-Home node.
     */
    fun isReportableImageUrl(url: HttpUrl): Boolean {
        val lowerHost = url.host.lowercase()
        if (lowerHost.contains("mangadex.org")) return false

        return url.pathSegments.any { segment ->
            segment == "data" || segment == "data-saver"
        }
    }

    /**
     * Chuyển byte count sang Int an toàn — clamp về [Int.MAX_VALUE] nếu vượt quá,
     * clamp về 0 nếu âm. Dùng cho At-Home report field `bytes`.
     */
    fun bytesToInt(bytes: Long): Int = min(bytes.coerceAtLeast(0L), Int.MAX_VALUE.toLong()).toInt()
}
