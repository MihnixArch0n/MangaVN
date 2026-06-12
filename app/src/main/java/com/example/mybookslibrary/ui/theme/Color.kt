package com.example.mybookslibrary.ui.theme

import androidx.compose.ui.graphics.Color

// Design system "Cinema" (refactor-ui-ux.md §3.1) — dark là GỐC, light phái sinh.
// Nền gần-đen pha xanh (tránh OLED smearing), tách lớp bằng 4 bậc luminance thay shadow;
// accent vermilion (ngôn ngữ thị giác manga/anime), tertiary gold cho rating.
// Mọi giá trị đã spot-check WCAG AA bằng scripts/check-contrast.sh — số ghi trong commit Phase 1.

// ---- Tối (gốc) ----
val CinemaDarkBackground = Color(0xFF0E0F13)
val CinemaDarkSurface = Color(0xFF16181D)
val CinemaDarkSurfaceContainer = Color(0xFF1E2128)
val CinemaDarkSurfaceContainerHigh = Color(0xFF262A33)
val CinemaDarkSurfaceContainerHighest = Color(0xFF2E3340)
val CinemaDarkSurfaceContainerLowest = Color(0xFF0B0C10)
val CinemaDarkPrimary = Color(0xFFFF5A3C)
val CinemaDarkOnPrimary = Color(0xFF1C0D08)
val CinemaDarkPrimaryContainer = Color(0xFF5C1A0C)
val CinemaDarkOnPrimaryContainer = Color(0xFFFFDAD2)
val CinemaDarkSecondary = Color(0xFFAEB4C0)
val CinemaDarkOnSecondary = Color(0xFF16181D)
val CinemaDarkSecondaryContainer = Color(0xFF262A33)
val CinemaDarkOnSecondaryContainer = Color(0xFFE9EAEE)
val CinemaDarkTertiary = Color(0xFFFFC24B)
val CinemaDarkOnTertiary = Color(0xFF221903)
val CinemaDarkTertiaryContainer = Color(0xFF4A3608)
val CinemaDarkOnTertiaryContainer = Color(0xFFFFE0A3)
val CinemaDarkOnSurface = Color(0xFFE9EAEE)
val CinemaDarkOnSurfaceVariant = Color(0xFF9BA0AC)
val CinemaDarkError = Color(0xFFFFB4AB)
val CinemaDarkOnError = Color(0xFF690005)
val CinemaDarkErrorContainer = Color(0xFF93000A)
val CinemaDarkOnErrorContainer = Color(0xFFFFDAD6)

// Border/divider mảnh: trắng 12% / 8% — không dùng màu đặc để không "đè" lên 4 bậc surface
val CinemaDarkOutline = Color(0x1FFFFFFF)
val CinemaDarkOutlineVariant = Color(0x14FFFFFF)

// ---- Sáng (phái sinh) ----
val CinemaLightBackground = Color(0xFFF7F7F9)
val CinemaLightSurface = Color(0xFFFFFFFF)

// surfaceContainer light = trắng + shadow mềm (Elevations trong Dimens.kt);
// Highest hơi xám cho vùng cần tách lớp không shadow (vd track của slider)
val CinemaLightSurfaceContainerHighest = Color(0xFFF2F2F5)
val CinemaLightSurfaceContainerLow = Color(0xFFFCFCFD)
val CinemaLightSurfaceDim = Color(0xFFE8E8EC)

// Primary đậm hơn dark primary để đạt AA trên nền sáng (§3.1)
val CinemaLightPrimary = Color(0xFFC8361B)
val CinemaLightOnPrimary = Color(0xFFFFFFFF)
val CinemaLightPrimaryContainer = Color(0xFFFFDAD2)
val CinemaLightOnPrimaryContainer = Color(0xFF3E0A00)
val CinemaLightSecondary = Color(0xFF5A5E68)
val CinemaLightOnSecondary = Color(0xFFFFFFFF)
val CinemaLightSecondaryContainer = Color(0xFFE8EAF0)
val CinemaLightOnSecondaryContainer = Color(0xFF1A1C22)
val CinemaLightTertiary = Color(0xFF9A6A00)
val CinemaLightOnTertiary = Color(0xFFFFFFFF)
val CinemaLightTertiaryContainer = Color(0xFFFFE2A8)
val CinemaLightOnTertiaryContainer = Color(0xFF312000)
val CinemaLightOnSurface = Color(0xFF1A1C22)
val CinemaLightOnSurfaceVariant = Color(0xFF5A5E68)
val CinemaLightSurfaceVariant = Color(0xFFEFEFF3)
val CinemaLightError = Color(0xFFBA1A1A)
val CinemaLightOnError = Color(0xFFFFFFFF)
val CinemaLightErrorContainer = Color(0xFFFFDAD6)
val CinemaLightOnErrorContainer = Color(0xFF410002)
val CinemaLightOutline = Color(0x1F000000)
val CinemaLightOutlineVariant = Color(0x14000000)
val CinemaLightInverseSurface = Color(0xFF2E3036)
val CinemaLightInverseOnSurface = Color(0xFFF0F0F4)

// ---- Màu trạng thái (giữ pattern cặp light/dark, giá trị theo palette Cinema) ----
val CinemaSuccess = Color(0xFF1E7A4C)
val CinemaWarning = Color(0xFF9C4500)
val CinemaDarkSuccess = Color(0xFF62D68F)
val CinemaDarkWarning = Color(0xFFFFA763)
val CinemaFavorite = Color(0xFFD32F2F)
val CinemaDarkFavorite = Color(0xFFFF6B6B)
