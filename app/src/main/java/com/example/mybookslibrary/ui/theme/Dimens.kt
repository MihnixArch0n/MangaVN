package com.example.mybookslibrary.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens — grid 4dp (refactor-ui-ux.md §3.3).
 * Mọi padding/gap trong UI lấy từ đây, không hardcode số lẻ.
 */
object Dimens {
    val SpacingXxs = 2.dp
    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 12.dp
    val SpacingLg = 16.dp
    val SpacingXl = 24.dp
    val SpacingXxl = 32.dp

    val ScreenPaddingCompact = 16.dp
    val ScreenPaddingMedium = 24.dp

    val IconXs = 14.dp
    val IconSm = 18.dp
    val IconMd = 20.dp
    val IconDefault = 24.dp
    val IconLg = 32.dp
    val IconXl = 48.dp
    val IconXxl = 56.dp
    val IconHero = 96.dp

    val AvatarSm = 40.dp
    val AvatarMd = 80.dp
    val AvatarLg = 100.dp

    val CoverThumb = 60.dp
    val CoverCard = 120.dp
    val CoverPreview = 160.dp

    val ControlButton = 36.dp
    val ActionButton = 40.dp

    val BorderThin = 1.dp
    val StrokeLight = 2.dp
    val StrokeMedium = 3.dp

    val DotActive = 10.dp
    val DotInactive = 8.dp
}

/**
 * Elevation tokens — CHỈ dùng ở light mode (shadow mềm);
 * dark mode tách lớp bằng 4 bậc surface container, KHÔNG shadow (§3.3).
 */
object Elevations {
    val Resting = 2.dp
    val Raised = 6.dp
    val Dialog = 8.dp
}
