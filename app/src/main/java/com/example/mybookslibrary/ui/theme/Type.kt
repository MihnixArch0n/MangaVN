// FontVariation (variable font axes) còn experimental trong compose-ui 1.11.2
@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.mybookslibrary.R

// Typography "Cinema" (refactor-ui-ux.md §3.2) — hybrid 2 font:
// Bricolage Grotesque (variable) cho display/headline lớn, Be Vietnam Pro cho title/body/label.
// Body lineHeight ≥1.45× vì dấu tiếng Việt 2 tầng ("ễ", "ậ") cần khoảng thở;
// labelLarge KHÔNG all-caps tracking lớn (chuỗi VI dài hơn EN ~35% → tràn nút).

private val BricolageGrotesque =
    FontFamily(
        Font(
            R.font.bricolage_grotesque,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            R.font.bricolage_grotesque,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

// Cut condensed (width axis 85) cho tên truyện dài tiếng Việt trên hero —
// co chiều ngang thay vì cắt chữ (dùng từ Phase 3, định nghĩa sẵn tại đây)
val BricolageGrotesqueCondensed =
    FontFamily(
        Font(
            R.font.bricolage_grotesque,
            weight = FontWeight.Bold,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(700),
                    FontVariation.width(85f),
                ),
        ),
    )

private val BeVietnamPro =
    FontFamily(
        Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
        Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
        Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
        Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
    )

val CinemaTypography =
    Typography(
        // Hero title (Discover/Detail) — tracking âm nhẹ cho chữ lớn
        displayLarge =
            TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.5).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.25).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        // Tiêu đề section lớn
        headlineLarge =
            TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        // Tiêu đề card / list item
        titleLarge =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            ),
        // Nội dung — lineHeight 1.5×
        bodyLarge =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),
        // Nút bấm — KHÔNG tracking lớn (chuỗi VI dài + dấu → tràn)
        labelLarge =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.25.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.25.sp,
            ),
    )

// Preview với chuỗi VI dài nhất thực tế trong strings.xml (§3.2 + checklist §4B Phase 1):
// hero displayLarge, nút labelLarge, body — không được tràn/cắt dấu 2 tầng.
@Composable
private fun TypographySpecimen() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(Dimens.ScreenPaddingCompact)) {
            Text(
                text = "Thám Tử Lừng Danh Và Những Đêm Trắng Ở Thị Trấn Ven Biển",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Tên đăng nhập và mật khẩu không được để trống",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.SpacingLg),
            )
            Text(
                text = "Đã có tài khoản? Đăng nhập",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Dimens.SpacingLg),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Typography VI dài nhất — light", showBackground = true)
@Composable
private fun TypographySpecimenLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) { TypographySpecimen() }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Typography VI dài nhất — dark", showBackground = true)
@Composable
private fun TypographySpecimenDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) { TypographySpecimen() }
}
