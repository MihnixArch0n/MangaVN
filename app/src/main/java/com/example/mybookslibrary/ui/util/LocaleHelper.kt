package com.example.mybookslibrary.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

// CompositionLocal chứa mã ngôn ngữ hiện tại — khi thay đổi, toàn bộ tree recompose
val LocalAppLocale = compositionLocalOf { "en" }

// Thay thế stringResource() — đọc string theo locale của app thay vì locale hệ thống
// Compose recompose khi LocalAppLocale thay đổi → chuyển ngôn ngữ mượt mà, không recreate Activity
@Composable
fun appString(@StringRes id: Int): String {
    val locale = LocalAppLocale.current
    val context = LocalContext.current
    return remember(locale, id) {
        context.localizedResources(locale).getString(id)
    }
}

@Composable
fun appString(@StringRes id: Int, vararg args: Any): String {
    val locale = LocalAppLocale.current
    val context = LocalContext.current
    val key = args.contentHashCode()
    return remember(locale, id, key) {
        context.localizedResources(locale).getString(id, *args)
    }
}

private fun Context.localizedResources(language: String): android.content.res.Resources {
    val config = resources.configuration.apply { setLocale(Locale(language)) }
    return createConfigurationContext(config).resources
}
