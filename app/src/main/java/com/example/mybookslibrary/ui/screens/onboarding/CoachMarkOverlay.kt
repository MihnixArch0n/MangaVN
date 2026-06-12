@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.screens.components.AppButton
import com.example.mybookslibrary.ui.screens.components.AppButtonStyle
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString

private const val OVERLAY_ALPHA = 0.78f
private val SPOTLIGHT_PADDING = 10.dp
private const val SPOTLIGHT_CORNER = 20f
private val TOOLTIP_MAX_WIDTH = 300.dp

/**
 * Coach mark overlay khoét sáng element thật.
 * Dùng Path + PathFillType.EvenOdd (addRect toàn màn + addRoundRect lỗ sáng)
 * — cách đáng tin nhất, không cần BlendMode.Clear/CompositingStrategy.
 *
 * Ref: https://www.droiddevtips.com/jetpack-compose-spotlight-effect-a-step-by-step-guide.html
 */
@Suppress("LongMethod")
@Composable
fun CoachMarkOverlay(
    visible: Boolean,
    state: CoachMarkState,
    steps: List<CoachMarkStep>,
    onDismiss: () -> Unit,
) {
    if (!visible || steps.isEmpty()) return

    val currentStep = state.currentStep.coerceIn(0, steps.lastIndex)
    val step = steps[currentStep]
    val targetRect = state.getTargetRect(step.key)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val spotlightPadPx = with(density) { SPOTLIGHT_PADDING.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { } },
    ) {
        // Overlay tối + khoét lỗ sáng bằng Path EvenOdd
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayPath =
                Path().apply {
                    // Rect toàn màn hình
                    addRect(Rect(Offset.Zero, Size(size.width, size.height)))
                    // Khoét lỗ sáng tại target
                    if (targetRect != null) {
                        addRoundRect(
                            RoundRect(
                                left = targetRect.left - spotlightPadPx,
                                top = targetRect.top - spotlightPadPx,
                                right = targetRect.right + spotlightPadPx,
                                bottom = targetRect.bottom + spotlightPadPx,
                                cornerRadius = CornerRadius(SPOTLIGHT_CORNER, SPOTLIGHT_CORNER),
                            ),
                        )
                    }
                    fillType = PathFillType.EvenOdd
                }
            drawPath(overlayPath, Color.Black.copy(alpha = OVERLAY_ALPHA))
        }

        // Tooltip + controls — đặt trên hoặc dưới target tuỳ vị trí
        val tooltipBelow = targetRect == null || targetRect.center.y < screenHeightPx / 2
        val tooltipY =
            if (targetRect == null) {
                with(density) { 200.dp.roundToPx() }
            } else if (tooltipBelow) {
                (targetRect.bottom + spotlightPadPx + with(density) { Dimens.SpacingLg.toPx() }).toInt()
            } else {
                (targetRect.top - spotlightPadPx - with(density) { 180.dp.toPx() }).toInt()
            }

        Column(
            modifier =
                Modifier
                    .offset { IntOffset(0, tooltipY) }
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingXl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 3 } + fadeOut())
                    }
                },
                label = "tourStep",
            ) { stepIndex ->
                val s = steps[stepIndex]
                Column(
                    modifier = Modifier.widthIn(max = TOOLTIP_MAX_WIDTH),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        appString(s.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Dimens.SpacingSm))
                    Text(
                        appString(s.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = Alphas.EmphasisVeryHigh),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.SpacingLg))
            Text(
                appString(R.string.tour_step_counter, currentStep + 1, steps.size),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = Alphas.EmphasisMuted),
            )
            Spacer(Modifier.height(Dimens.SpacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (currentStep > 0) {
                    AppButton(
                        text = appString(R.string.tour_back),
                        onClick = { state.currentStep-- },
                        style = AppButtonStyle.Text,
                    )
                }
                AppButton(
                    text =
                        if (currentStep == steps.lastIndex) {
                            appString(R.string.tour_done)
                        } else {
                            appString(R.string.tour_next)
                        },
                    onClick = {
                        if (currentStep == steps.lastIndex) onDismiss() else state.currentStep++
                    },
                )
                AppButton(
                    text = appString(R.string.tour_skip),
                    onClick = onDismiss,
                    style = AppButtonStyle.Text,
                )
            }
        }
    }
}
