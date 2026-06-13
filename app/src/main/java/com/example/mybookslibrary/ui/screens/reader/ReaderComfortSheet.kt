@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReaderBackground
import com.example.mybookslibrary.ui.screens.components.AppFilterChip
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString

// Độ sáng overlay tối thiểu/tối đa — đồng bộ với ReaderViewModel (chỉ làm tối, không sáng hơn hệ thống).
private const val MIN_BRIGHTNESS = 0.15f
private const val MAX_BRIGHTNESS = 1.0f

/**
 * Bottom sheet tuỳ chỉnh hiển thị khi đọc: độ sáng overlay + màu nền.
 *
 * Mở từ nút trên thanh điều khiển dưới của reader. Dùng [ModalBottomSheet] (render window riêng)
 * thay vì panel inline để nhất quán với PageActionBottomSheet và tránh refactor bottom bar.
 *
 * @param brightness Độ sáng hiện tại (0.15..1.0).
 * @param background Màu nền đang chọn.
 * @param onBrightnessChange Gọi khi kéo slider (preview tức thời, không ghi DataStore).
 * @param onBrightnessChangeFinished Gọi khi thả slider (lưu DataStore một lần, tránh ghi mỗi tick).
 * @param onBackgroundChange Gọi khi chọn màu nền mới.
 * @param onDismiss Gọi khi sheet bị đóng.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReaderComfortSheet(
    brightness: Float,
    background: ReaderBackground,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onBackgroundChange: (ReaderBackground) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ReaderComfortPanelContent(
            brightness = brightness,
            background = background,
            onBrightnessChange = onBrightnessChange,
            onBrightnessChangeFinished = onBrightnessChangeFinished,
            onBackgroundChange = onBackgroundChange,
        )
    }
}

@Composable
private fun ReaderComfortPanelContent(
    brightness: Float,
    background: ReaderBackground,
    onBrightnessChange: (Float) -> Unit,
    onBackgroundChange: (ReaderBackground) -> Unit,
    onBrightnessChangeFinished: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPaddingCompact, vertical = Dimens.SpacingLg),
    ) {
        Text(
            text = appString(R.string.reader_brightness),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            onValueChangeFinished = onBrightnessChangeFinished,
            valueRange = MIN_BRIGHTNESS..MAX_BRIGHTNESS,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = appString(R.string.reader_background),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Dimens.SpacingMd),
        )
        Row(
            modifier = Modifier.padding(top = Dimens.SpacingSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        ) {
            ReaderBackground.entries.forEach { option ->
                AppFilterChip(
                    label = appString(option.labelRes()),
                    selected = option == background,
                    onClick = { onBackgroundChange(option) },
                )
            }
        }
    }
}

/** Chuỗi nhãn hiển thị cho từng màu nền. */
internal fun ReaderBackground.labelRes(): Int =
    when (this) {
        ReaderBackground.BLACK -> R.string.reader_background_black
        ReaderBackground.WHITE -> R.string.reader_background_white
        ReaderBackground.GRAY -> R.string.reader_background_gray
    }

@Suppress("UnusedPrivateMember")
@Preview(name = "Reader comfort panel", showBackground = true)
@Composable
private fun ReaderComfortPanelPreview() {
    MyBooksLibraryTheme {
        ReaderComfortPanelContent(
            brightness = 0.7f,
            background = ReaderBackground.BLACK,
            onBrightnessChange = {},
            onBackgroundChange = {},
        )
    }
}
