package com.example.mybookslibrary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KansoLightScheme = lightColorScheme(
    primary = KansoInk,
    onPrimary = KansoCard,
    primaryContainer = KansoInk,
    onPrimaryContainer = KansoCard,
    secondary = KansoGraphite,
    onSecondary = KansoCard,
    secondaryContainer = KansoPaper,
    onSecondaryContainer = KansoSoftInk,
    tertiary = KansoTerracotta,
    onTertiary = KansoCard,
    background = KansoPaper,
    onBackground = KansoSoftInk,
    surface = KansoCard,
    onSurface = KansoSoftInk,
    surfaceVariant = KansoPaper,
    onSurfaceVariant = KansoGraphite,
    error = KansoTerracotta,
    onError = KansoCard,
    outline = KansoGraphite,
    outlineVariant = KansoCard,
    inverseSurface = KansoInk,
    inverseOnSurface = KansoCard
)

private val KansoDarkScheme = darkColorScheme(
    primary = KansoDarkOnSurface,
    onPrimary = KansoDarkBackground,
    primaryContainer = KansoDarkElevated,
    onPrimaryContainer = KansoDarkOnSurface,
    secondary = KansoDarkMuted,
    onSecondary = KansoDarkBackground,
    secondaryContainer = KansoDarkSurface,
    onSecondaryContainer = KansoDarkOnSurface,
    tertiary = KansoDarkTerracotta,
    onTertiary = KansoDarkBackground,
    background = KansoDarkBackground,
    onBackground = KansoDarkOnSurface,
    surface = KansoDarkCard,
    onSurface = KansoDarkOnSurface,
    surfaceVariant = KansoDarkSurface,
    onSurfaceVariant = KansoDarkMuted,
    error = KansoDarkTerracotta,
    onError = KansoDarkBackground,
    outline = KansoDarkMuted,
    outlineVariant = KansoDarkCard,
    inverseSurface = KansoDarkOnSurface,
    inverseOnSurface = KansoDarkBackground
)

@Composable
fun MyBooksLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KansoDarkScheme else KansoLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KansoTypography,
        content = content
    )
}
