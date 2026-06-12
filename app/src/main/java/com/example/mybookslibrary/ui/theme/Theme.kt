package com.example.mybookslibrary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Scheme "Cinema" (refactor-ui-ux.md §3.1 + §7): dark là gốc — tách lớp bằng
// 4 bậc surface container, TẮT surfaceTint (= surface, tránh ám tím M3);
// light phái sinh — trắng + shadow mềm (Elevations), không dựa tonal color.

private val CinemaDarkScheme =
    darkColorScheme(
        primary = CinemaDarkPrimary,
        onPrimary = CinemaDarkOnPrimary,
        primaryContainer = CinemaDarkPrimaryContainer,
        onPrimaryContainer = CinemaDarkOnPrimaryContainer,
        inversePrimary = CinemaLightPrimary,
        secondary = CinemaDarkSecondary,
        onSecondary = CinemaDarkOnSecondary,
        secondaryContainer = CinemaDarkSecondaryContainer,
        onSecondaryContainer = CinemaDarkOnSecondaryContainer,
        tertiary = CinemaDarkTertiary,
        onTertiary = CinemaDarkOnTertiary,
        tertiaryContainer = CinemaDarkTertiaryContainer,
        onTertiaryContainer = CinemaDarkOnTertiaryContainer,
        background = CinemaDarkBackground,
        onBackground = CinemaDarkOnSurface,
        surface = CinemaDarkSurface,
        onSurface = CinemaDarkOnSurface,
        surfaceVariant = CinemaDarkSurfaceContainer,
        onSurfaceVariant = CinemaDarkOnSurfaceVariant,
        surfaceTint = CinemaDarkSurface,
        inverseSurface = CinemaDarkOnSurface,
        inverseOnSurface = CinemaLightOnSurface,
        error = CinemaDarkError,
        onError = CinemaDarkOnError,
        errorContainer = CinemaDarkErrorContainer,
        onErrorContainer = CinemaDarkOnErrorContainer,
        outline = CinemaDarkOutline,
        outlineVariant = CinemaDarkOutlineVariant,
        surfaceBright = CinemaDarkSurfaceContainerHighest,
        surfaceDim = CinemaDarkBackground,
        surfaceContainer = CinemaDarkSurfaceContainer,
        surfaceContainerHigh = CinemaDarkSurfaceContainerHigh,
        surfaceContainerHighest = CinemaDarkSurfaceContainerHighest,
        surfaceContainerLow = CinemaDarkSurface,
        surfaceContainerLowest = CinemaDarkSurfaceContainerLowest,
    )

private val CinemaLightScheme =
    lightColorScheme(
        primary = CinemaLightPrimary,
        onPrimary = CinemaLightOnPrimary,
        primaryContainer = CinemaLightPrimaryContainer,
        onPrimaryContainer = CinemaLightOnPrimaryContainer,
        inversePrimary = CinemaDarkPrimary,
        secondary = CinemaLightSecondary,
        onSecondary = CinemaLightOnSecondary,
        secondaryContainer = CinemaLightSecondaryContainer,
        onSecondaryContainer = CinemaLightOnSecondaryContainer,
        tertiary = CinemaLightTertiary,
        onTertiary = CinemaLightOnTertiary,
        tertiaryContainer = CinemaLightTertiaryContainer,
        onTertiaryContainer = CinemaLightOnTertiaryContainer,
        background = CinemaLightBackground,
        onBackground = CinemaLightOnSurface,
        surface = CinemaLightSurface,
        onSurface = CinemaLightOnSurface,
        surfaceVariant = CinemaLightSurfaceVariant,
        onSurfaceVariant = CinemaLightOnSurfaceVariant,
        surfaceTint = CinemaLightSurface,
        inverseSurface = CinemaLightInverseSurface,
        inverseOnSurface = CinemaLightInverseOnSurface,
        error = CinemaLightError,
        onError = CinemaLightOnError,
        errorContainer = CinemaLightErrorContainer,
        onErrorContainer = CinemaLightOnErrorContainer,
        outline = CinemaLightOutline,
        outlineVariant = CinemaLightOutlineVariant,
        surfaceBright = CinemaLightSurface,
        surfaceDim = CinemaLightSurfaceDim,
        surfaceContainer = CinemaLightSurface,
        surfaceContainerHigh = CinemaLightSurface,
        surfaceContainerHighest = CinemaLightSurfaceContainerHighest,
        surfaceContainerLow = CinemaLightSurfaceContainerLow,
        surfaceContainerLowest = CinemaLightSurface,
    )

/**
 * Màu semantic ngoài chuẩn M3 (success/warning) — theme tự chọn biến thể light/dark,
 * screens KHÔNG tự branch (trước đây StatusChip so sánh background để đoán dark mode).
 */
@Immutable
data class StatusColors(
    val success: Color,
    val warning: Color,
    val favorite: Color,
)

private val LightStatusColors =
    StatusColors(success = CinemaSuccess, warning = CinemaWarning, favorite = CinemaFavorite)
private val DarkStatusColors =
    StatusColors(success = CinemaDarkSuccess, warning = CinemaDarkWarning, favorite = CinemaDarkFavorite)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Truy cập StatusColors theo theme hiện hành — dùng như MaterialTheme.colorScheme */
val MaterialTheme.statusColors: StatusColors
    @Composable get() = LocalStatusColors.current

@Composable
fun MyBooksLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CinemaDarkScheme else CinemaLightScheme
    CompositionLocalProvider(
        LocalReducedMotion provides rememberReducedMotion(),
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        // MotionScheme.expressive() còn INTERNAL trong material3 1.4.0 (BOM 2026.05.01,
        // compiler xác nhận 2026-06-11) — dùng motion mặc định M3, KHÔNG tự chế curve.
        // Quyết bump material3 1.5+ (expressive public) tại GATE 1 — xem refactor-ui-ux.md §6.
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = CinemaShapes,
            typography = CinemaTypography,
            content = content,
        )
    }
}
