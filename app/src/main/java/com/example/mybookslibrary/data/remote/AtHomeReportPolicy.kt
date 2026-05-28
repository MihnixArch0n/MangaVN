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

    fun isReportableImageUrl(rawUrl: String): Boolean {
        return rawUrl.toHttpUrlOrNull()?.let(::isReportableImageUrl) == true
    }

    fun isReportableImageUrl(url: HttpUrl): Boolean {
        val lowerHost = url.host.lowercase()
        if (lowerHost.contains("mangadex.org")) return false

        return url.pathSegments.any { segment ->
            segment == "data" || segment == "data-saver"
        }
    }

    fun bytesToInt(bytes: Long): Int {
        return min(bytes.coerceAtLeast(0L), Int.MAX_VALUE.toLong()).toInt()
    }
}
