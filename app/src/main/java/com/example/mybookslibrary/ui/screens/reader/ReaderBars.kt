@file:Suppress(
    "LongMethod",
    "LongParameterList",
    "MagicNumber",
    "MaxLineLength",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.BookOpenCheck
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scroll
import com.composables.icons.lucide.Sun
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString
import timber.log.Timber

private const val DARK_READER_BAR_CONTAINER_ALPHA = 0.94f
private val READER_SLIDER_THUMB_SIZE = 20.dp
private val READER_SLIDER_TRACK_HEIGHT = 4.dp
private val READER_SLIDER_LABEL_SIZE = 40.dp
private val READER_SLIDER_LABEL_OFFSET = (-44).dp

internal data class ReaderBarColors(
    val container: Color,
    val content: Color,
    val secondaryContent: Color,
    val controlContainer: Color,
)

internal data class ReaderBottomBarState(
    val currentPage: Int,
    val totalPages: Int,
    val currentReadingMode: ReadingMode,
    val hasPrevChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
)

@Composable
internal fun readerBarColors(
    isLightTheme: Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f
): ReaderBarColors {
    val colorScheme = MaterialTheme.colorScheme
    if (isLightTheme) {
        return ReaderBarColors(
            container = colorScheme.background,
            content = colorScheme.onBackground,
            secondaryContent = colorScheme.onSurfaceVariant,
            controlContainer = colorScheme.onBackground.copy(alpha = Alphas.ContainerFaint),
        )
    }

    return ReaderBarColors(
        container = colorScheme.surface.copy(alpha = DARK_READER_BAR_CONTAINER_ALPHA),
        content = colorScheme.onSurface,
        secondaryContent = colorScheme.onSurfaceVariant,
        controlContainer = colorScheme.onSurface.copy(alpha = Alphas.ContainerSelected),
    )
}

/**
 * Top reader overlay bar anchored inside the parent [BoxScope].
 *
 * Shows the chapter title and a back button only while [isVisible] is true.
 *
 * @param chapterTitle Title rendered in the center area of the bar.
 * @param isVisible Controls the animated visibility of the bar.
 * @param onBackClick Called when the back button is tapped.
 */
@Composable
internal fun BoxScope.ReaderTopBar(
    chapterTitle: String,
    isVisible: Boolean,
    colors: ReaderBarColors = readerBarColors(),
    onBackClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.container)
                    .consumeReaderOverlayGestures()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                Timber.v("ReaderTopBar back clicked")
                onBackClick()
            }) {
                Box(
                    modifier =
                        Modifier
                            .size(Dimens.ControlButton)
                            .clip(CircleShape)
                            .background(colors.controlContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Lucide.ArrowLeft,
                        contentDescription = appString(R.string.cd_back),
                        tint = colors.content,
                        modifier = Modifier.size(Dimens.IconMd),
                    )
                }
            }
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.titleMedium,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = Dimens.SpacingSm),
            )
        }
    }
}

@Preview(name = "Reader Top Bar", showBackground = true)
@Composable
private fun ReaderTopBarPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            ReaderTopBar(
                chapterTitle = "Chapter 12: Lost Pages",
                isVisible = true,
                onBackClick = { },
            )
        }
    }
}

@Preview(name = "Reader Bottom Bar", showBackground = true)
@Composable
private fun ReaderBottomBarPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            ReaderBottomBar(
                isVisible = true,
                state =
                    ReaderBottomBarState(
                        currentPage = 1,
                        totalPages = 18,
                        currentReadingMode = ReadingMode.LTR,
                    ),
                onToggleReadingMode = { },
            )
        }
    }
}

/**
 * Bottom reader overlay bar anchored inside the parent [BoxScope].
 *
 * Displays the current page counter, a quick page slider, and a reading-mode toggle button.
 * The page counter is rendered as one-based and clamped to the valid range.
 *
 * @param isVisible Controls the animated visibility of the bar.
 * @param currentPage Zero-based current page index used to derive the displayed counter.
 * @param totalPages Total number of pages available in the chapter.
 * @param currentReadingMode Active reading mode used to choose the toggle icon and label.
 * @param onToggleReadingMode Called when the mode toggle button is tapped.
 * @param onPageSelected Called with the zero-based target page after the slider is released.
 */
@Composable
internal fun BoxScope.ReaderBottomBar(
    isVisible: Boolean,
    currentPage: Int,
    totalPages: Int,
    currentReadingMode: ReadingMode,
    colors: ReaderBarColors = readerBarColors(),
    onToggleReadingMode: () -> Unit,
    onPageSelected: (Int) -> Unit = {},
) {
    ReaderBottomBar(
        isVisible = isVisible,
        state =
            ReaderBottomBarState(
                currentPage = currentPage,
                totalPages = totalPages,
                currentReadingMode = currentReadingMode,
            ),
        colors = colors,
        onToggleReadingMode = onToggleReadingMode,
        onPageSelected = onPageSelected,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun BoxScope.ReaderBottomBar(
    isVisible: Boolean,
    state: ReaderBottomBarState,
    colors: ReaderBarColors = readerBarColors(),
    onToggleReadingMode: () -> Unit,
    onPageSelected: (Int) -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onComfortClick: () -> Unit = {},
) {
    val safeTotalPages = state.totalPages.coerceAtLeast(1)
    val displayPage = (state.currentPage + 1).coerceIn(1, safeTotalPages)
    val currentPageIndex = displayPage - 1
    var sliderPage by remember(currentPageIndex, safeTotalPages) {
        mutableFloatStateOf(currentPageIndex.toFloat())
    }
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isSliderDragged by sliderInteractionSource.collectIsDraggedAsState()
    val sliderThumbColor = MaterialTheme.colorScheme.primary
    val sliderLabelContentColor = MaterialTheme.colorScheme.onPrimary
    val sliderColors =
        SliderDefaults.colors(
            thumbColor = sliderThumbColor,
            activeTrackColor = sliderThumbColor,
            inactiveTrackColor = colors.secondaryContent.copy(alpha = Alphas.ContainerSelected),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        )
    val nextReadingModeRes =
        when (state.currentReadingMode) {
            ReadingMode.VERTICAL -> R.string.reader_mode_ltr
            ReadingMode.LTR -> R.string.reader_mode_rtl
            ReadingMode.RTL -> R.string.reader_mode_vertical
        }
    val currentReadingModeLabelRes =
        when (state.currentReadingMode) {
            ReadingMode.VERTICAL -> R.string.reader_mode_label_vertical
            ReadingMode.LTR -> R.string.reader_mode_label_ltr
            ReadingMode.RTL -> R.string.reader_mode_label_rtl
        }

    val modeIcon =
        when (state.currentReadingMode) {
            ReadingMode.VERTICAL -> Lucide.Scroll
            ReadingMode.LTR -> Lucide.BookOpen
            ReadingMode.RTL -> Lucide.BookOpenCheck
        }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.container)
                    .consumeReaderOverlayGestures()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.ScreenPaddingCompact, vertical = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevChapter,
                enabled = state.hasPrevChapter,
            ) {
                Icon(
                    Lucide.ChevronLeft,
                    contentDescription = appString(R.string.reader_prev_chapter),
                    tint =
                        if (state.hasPrevChapter) {
                            colors.content
                        } else {
                            colors.content.copy(alpha = Alphas.ContainerFaint)
                        },
                    modifier = Modifier.size(Dimens.IconDefault),
                )
            }
            Text(
                text = "$displayPage / $safeTotalPages",
                style = MaterialTheme.typography.titleMedium,
                color = colors.content,
            )
            ReaderPageSlider(
                page = sliderPage,
                totalPages = safeTotalPages,
                isEnabled = state.totalPages > 1,
                interactionSource = sliderInteractionSource,
                isDragged = isSliderDragged,
                thumbColor = sliderThumbColor,
                labelContentColor = sliderLabelContentColor,
                sliderColors = sliderColors,
                onPageChange = { sliderPage = it },
                onPageChangeFinished = {
                    onPageSelected(sliderPage.toInt().coerceIn(0, safeTotalPages - 1))
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.SpacingSm),
            )
            TextButton(
                onClick = {
                    Timber.v("ReaderBottomBar toggle clicked: currentMode=%s", state.currentReadingMode)
                    onToggleReadingMode()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = colors.content),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription =
                        appString(
                            R.string.reader_switch_mode_action,
                            appString(nextReadingModeRes),
                        ),
                    tint = colors.content,
                    modifier = Modifier.size(Dimens.IconDefault),
                )
                Text(
                    text = appString(currentReadingModeLabelRes),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = Dimens.SpacingSm),
                )
            }
            IconButton(onClick = onComfortClick) {
                Icon(
                    Lucide.Sun,
                    contentDescription = appString(R.string.reader_comfort_settings),
                    tint = colors.content,
                    modifier = Modifier.size(Dimens.IconDefault),
                )
            }
            IconButton(
                onClick = onNextChapter,
                enabled = state.hasNextChapter,
            ) {
                Icon(
                    Lucide.ChevronRight,
                    contentDescription = appString(R.string.reader_next_chapter),
                    tint =
                        if (state.hasNextChapter) {
                            colors.content
                        } else {
                            colors.content.copy(alpha = Alphas.ContainerFaint)
                        },
                    modifier = Modifier.size(Dimens.IconDefault),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReaderPageSlider(
    page: Float,
    totalPages: Int,
    isEnabled: Boolean,
    interactionSource: MutableInteractionSource,
    isDragged: Boolean,
    thumbColor: Color,
    labelContentColor: Color,
    sliderColors: androidx.compose.material3.SliderColors,
    onPageChange: (Float) -> Unit,
    onPageChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val pageFraction = if (totalPages > 1) page / (totalPages - 1) else 0f
        val thumbCenter =
            READER_SLIDER_THUMB_SIZE / 2 +
                (maxWidth - READER_SLIDER_THUMB_SIZE) * pageFraction
        val labelOffset = thumbCenter - READER_SLIDER_LABEL_SIZE / 2

        Slider(
            value = page,
            onValueChange = onPageChange,
            onValueChangeFinished = onPageChangeFinished,
            valueRange = 0f..(totalPages - 1).toFloat(),
            steps = (totalPages - 2).coerceAtLeast(0),
            enabled = isEnabled,
            interactionSource = interactionSource,
            thumb = {
                Box(
                    modifier =
                        Modifier
                            .size(READER_SLIDER_THUMB_SIZE)
                            .clip(CircleShape)
                            .background(thumbColor),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = sliderColors,
                    modifier = Modifier.height(READER_SLIDER_TRACK_HEIGHT),
                    thumbTrackGapSize = 0.dp,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = isDragged,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = labelOffset, y = READER_SLIDER_LABEL_OFFSET),
        ) {
            Surface(
                color = thumbColor,
                contentColor = labelContentColor,
                shape = CircleShape,
                modifier = Modifier.size(READER_SLIDER_LABEL_SIZE),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (page.toInt() + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun Modifier.consumeReaderOverlayGestures(): Modifier =
    pointerInput(Unit) {
        detectTapGestures(onTap = {})
    }
