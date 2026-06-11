package com.example.mybookslibrary.util

import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import androidx.core.net.toUri
import android.provider.Settings

private const val MANGADEX_BASE_URL = "https://mangadex.org/title"
private val MANGADEX_TITLE_REGEX = Regex("""https://mangadex\.org/title/([^/\s]+)""")

/** Builds the canonical MangaDex title URL for [mangaId]. */
fun buildMangaDexTitleUrl(mangaId: String): String = "$MANGADEX_BASE_URL/$mangaId"

/** Builds the share text shown in the Android chooser. */
fun buildShareText(mangaTitle: String, mangaId: String): String =
    "Đọc truyện $mangaTitle: ${buildMangaDexTitleUrl(mangaId)}"

/**
 * Tries to extract a MangaDex manga ID from a URL string.
 * Handles both `https://mangadex.org/title/{id}` and `.../title/{id}/slug` formats.
 * Returns null if the text is not a recognisable MangaDex title URL.
 */
fun extractMangaIdFromMangaDexUrl(text: String?): String? =
    text?.let { MANGADEX_TITLE_REGEX.find(it)?.groupValues?.getOrNull(1) }

/**
 * Returns true if the app is approved to open supported links (mangadex.org) automatically.
 *
 * On Android 12+ (API 31), uses [DomainVerificationManager] to query the user's
 * per-domain selection state. On older versions always returns false because the
 * disambiguation dialog handles it instead.
 */
fun isOpenLinksGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val manager = context.getSystemService(DomainVerificationManager::class.java) ?: return false
    val userState = manager.getDomainVerificationUserState(context.packageName) ?: return false
    // SELECTED means the user has explicitly chosen this app to open these domains.
    return userState.hostToStateMap.values.any { state ->
        state == DomainVerificationUserState.DOMAIN_STATE_SELECTED
    }
}

/**
 * Opens the system Settings screen where the user can enable "Open by default" /
 * "Open supported links" for this app.
 *
 * - Android 12+ → ACTION_APP_OPEN_BY_DEFAULT_SETTINGS (deep link to Open by default page)
 * - Older → ACTION_APPLICATION_DETAILS_SETTINGS (generic app info page)
 */
fun openAppLinkSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            "package:${context.packageName}".toUri(),
        )
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri(),
        )
    }
    context.startActivity(intent)
}

/**
 * Opens the native Android share sheet for a manga.
 *
 * Keep Android platform APIs (Intent, Context) isolated here so that
 * Compose screens remain free of platform coupling and share logic is
 * independently unit-testable via [buildMangaDexTitleUrl] / [buildShareText].
 */
fun shareManga(context: Context, mangaId: String, mangaTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, buildShareText(mangaTitle, mangaId))
    }
    context.startActivity(Intent.createChooser(intent, null))
}
