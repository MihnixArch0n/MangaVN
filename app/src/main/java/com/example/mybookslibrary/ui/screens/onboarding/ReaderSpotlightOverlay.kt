package com.example.mybookslibrary.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.screens.components.AppButton
import com.example.mybookslibrary.ui.screens.components.AppButtonStyle
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString

private const val OVERLAY_ALPHA = 0.75f

/**
 * Overlay 2 bước hướng dẫn reader: (1) tap zones (trái/phải/giữa), (2) nút đổi chế độ đọc.
 * Hiển thị lần đầu vào Reader khi `reader_hint_done = false`.
 */
@Composable
fun ReaderSpotlightOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        var step by remember { mutableIntStateOf(0) }
        val hintText =
            when (step) {
                0 -> appString(R.string.reader_hint_tap_sides)
                else -> appString(R.string.reader_hint_mode_toggle)
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (step < 1) step++ else onDismiss()
                    }
                    .semantics { contentDescription = hintText },
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = Dimens.SpacingXl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    hintText,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = Dimens.SpacingXl),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AppButton(
                        text = appString(R.string.reader_hint_got_it),
                        onClick = { if (step < 1) step++ else onDismiss() },
                    )
                    AppButton(
                        text = appString(R.string.reader_hint_skip),
                        onClick = onDismiss,
                        style = AppButtonStyle.Text,
                    )
                }
            }
        }
    }
}
