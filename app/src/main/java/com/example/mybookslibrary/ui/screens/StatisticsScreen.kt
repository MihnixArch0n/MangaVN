@file:Suppress("TooManyFunctions")

package com.example.mybookslibrary.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChartNoAxesColumn
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.dao.TopMangaCount
import com.example.mybookslibrary.ui.screens.components.EmptyState
import com.example.mybookslibrary.ui.screens.components.SectionHeader
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.statusColors
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.util.isLandscape
import com.example.mybookslibrary.ui.viewmodel.StatisticsViewModel
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.VerticalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.Pie
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    vm: StatisticsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Lucide.ArrowLeft, contentDescription = appString(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = Dimens.ScreenPaddingCompact,
                vertical = Dimens.SpacingLg,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
        ) {
            item {
                ChapterSummaryRow(
                    total = state.totalChapters,
                    completed = state.completedChapters,
                    inProgress = state.inProgressChapters,
                )
            }
            val hasWeeklyData = state.weeklyActivity.any { it > 0 }
            val hasMonthlyData = state.monthlyTrend.any { it > 0 }
            val hasActivity = hasWeeklyData || hasMonthlyData
            // Mọi item đều có status READING/COMPLETED (favorite chỉ là cờ chồng lấp)
            // → reading + completed = tổng thư viện, là điều kiện đúng để vẽ pie.
            val hasLibrary = state.readingCount + state.completedCount > 0

            if (hasWeeklyData) {
                item { SectionHeader(title = appString(R.string.stats_weekly_activity)) }
                item { WeeklyColumnChart(activity = state.weeklyActivity) }
            }
            if (hasMonthlyData) {
                item { SectionHeader(title = appString(R.string.stats_monthly_trend)) }
                item { MonthlyLineChart(trend = state.monthlyTrend) }
            }
            if (hasLibrary) {
                item { SectionHeader(title = appString(R.string.stats_library_breakdown)) }
                item {
                    LibraryPieChart(
                        reading = state.readingCount,
                        completed = state.completedCount,
                    )
                }
                // Yêu thích là metric độc lập (chồng lấp Đang đọc/Hoàn thành) → card riêng,
                // không phải múi pie — pie chỉ phân hoạch theo status. Hiện cả khi = 0
                // (có chủ đích, nhất quán với legend pie cũng hiện count 0).
                item {
                    SummaryCard(
                        state.favoriteCount.toString(),
                        appString(R.string.stats_favorite),
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.topManga.isNotEmpty()) {
                item { SectionHeader(title = appString(R.string.stats_top_manga)) }
                item { TopMangaRowChart(items = state.topManga) }
            }
            if (!hasActivity && !hasLibrary) {
                item {
                    EmptyState(
                        title = appString(R.string.stats_no_data),
                        icon = Lucide.ChartNoAxesColumn,
                        modifier = Modifier.fillParentMaxHeight(0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterSummaryRow(total: Int, completed: Int, inProgress: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
    ) {
        SummaryCard(
            total.toString(),
            appString(R.string.stats_total_chapters),
            Modifier.weight(1f).fillMaxHeight(),
        )
        SummaryCard(
            completed.toString(),
            appString(R.string.stats_completed_chapters),
            Modifier.weight(1f).fillMaxHeight(),
        )
        SummaryCard(
            inProgress.toString(),
            appString(R.string.stats_in_progress_chapters),
            Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Số ≥5 chữ số tràn card 1/3 màn hình ở headlineLarge → hạ cỡ chữ
            val valueStyle = if (value.length > SUMMARY_VALUE_MAX_DIGITS) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineLarge
            }
            Text(
                value,
                style = valueStyle,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Spacer(Modifier.height(Dimens.SpacingXs))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeeklyColumnChart(activity: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val today = LocalDate.now()
    val labels = remember(today) { weekDayLabels() }
    val seriesLabel = appString(R.string.stats_chapters_read)

    val barsData = remember(activity, seriesLabel) {
        activity.mapIndexed { index, value ->
            Bars(
                label = labels.getOrElse(index) { "" },
                values = listOf(
                    Bars.Data(
                        label = seriesLabel,
                        value = value.toDouble().coerceAtLeast(0.0),
                        color = SolidColor(primary),
                    ),
                ),
            )
        }
    }

    val chartHeight = if (isLandscape()) 140.dp else 180.dp
    ChartCard {
        ColumnChart(
            modifier = Modifier.fillMaxWidth().height(chartHeight).padding(Dimens.SpacingSm),
            data = barsData,
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
                spacing = 4.dp,
                thickness = 24.dp,
            ),
            gridProperties = themedGridProperties(),
            labelProperties = themedLabelProperties(),
            labelHelperProperties = themedLabelHelperProperties(),
            indicatorProperties = themedIndicatorProperties(maxValue = activity.max()),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
}

@Composable
private fun MonthlyLineChart(trend: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val chaptersLabel = appString(R.string.stats_chapters_read)

    val lineData = remember(trend, chaptersLabel) {
        listOf(
            Line(
                label = chaptersLabel,
                values = trend.map { it.toDouble().coerceAtLeast(0.0) },
                color = SolidColor(primary),
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(primary),
                    strokeWidth = 2.dp,
                    radius = 4.dp,
                ),
                drawStyle = DrawStyle.Stroke(width = 3.dp),
            ),
        )
    }

    // Nhãn trục X: 3 tuần trước → tuần này (khớp thứ tự buildMonthlyTrend)
    val weekLabels = listOf(
        appString(R.string.stats_weeks_ago, 3),
        appString(R.string.stats_weeks_ago, 2),
        appString(R.string.stats_week_last),
        appString(R.string.stats_week_this),
    )
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val lineChartHeight = if (isLandscape()) 140.dp else 180.dp
    ChartCard {
        LineChart(
            modifier = Modifier.fillMaxWidth().height(lineChartHeight).padding(Dimens.SpacingSm),
            data = lineData,
            gridProperties = themedGridProperties(),
            labelProperties = LabelProperties(
                enabled = true,
                textStyle = labelStyle,
                labels = weekLabels,
            ),
            labelHelperProperties = themedLabelHelperProperties(),
            indicatorProperties = themedIndicatorProperties(maxValue = trend.max()),
        )
    }
}

@Composable
private fun LibraryPieChart(reading: Int, completed: Int) {
    val total = reading + completed
    // Pie là PHÂN HOẠCH theo status (2 múi, tổng = số truyện trong thư viện).
    // Cùng bảng màu với StatusChip: Đang đọc = tertiary, Hoàn thành = success.
    val readingColor = MaterialTheme.colorScheme.tertiary
    val completedColor = MaterialTheme.statusColors.success
    val rl = appString(R.string.stats_reading)
    val cl = appString(R.string.stats_completed)

    // Key đủ cả màu: theme đổi (dark/light) giữa chừng phải rebuild pie với màu mới
    var pieData by remember(reading, completed, rl, cl, readingColor, completedColor) {
        mutableStateOf(
            listOfNotNull(
                if (reading > 0) {
                    Pie(label = rl, data = reading.toDouble(), color = readingColor, selectedColor = readingColor)
                } else {
                    null
                },
                if (completed > 0) {
                    Pie(label = cl, data = completed.toDouble(), color = completedColor, selectedColor = completedColor)
                } else {
                    null
                },
            ),
        )
    }

    ChartCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (total > 0) {
                Box(contentAlignment = Alignment.Center) {
                    val pieSize = if (isLandscape()) 140.dp else 180.dp
                    PieChart(
                        modifier = Modifier.size(pieSize),
                        data = pieData,
                        onPieClick = { clicked ->
                            // Tap slice → popup label + số lượng giữa chart (tap lại để ẩn)
                            pieData = pieData.map {
                                it.copy(selected = it.label == clicked.label && !it.selected)
                            }
                        },
                        // Phóng nhẹ slice đang chọn để user nhận biết ngoài popup giữa chart
                        selectedScale = 1.08f,
                        labelHelperProperties = LabelHelperProperties(enabled = false),
                        scaleAnimEnterSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                    val selected = pieData.firstOrNull { it.selected }
                    if (selected != null) {
                        Text(
                            text = "${selected.label}: ${selected.data.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.inverseSurface)
                                .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingLg))
            }
            BreakdownLegend(
                items = listOf(
                    LegendItem(readingColor, appString(R.string.stats_reading), reading),
                    LegendItem(completedColor, appString(R.string.stats_completed), completed),
                ),
            )
        }
    }
}

@Composable
private fun TopMangaRowChart(items: List<TopMangaCount>) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val rowData = remember(items, primaryColor) {
        items.mapIndexed { index, manga ->
            Bars(
                label = manga.title.ellipsize(MAX_TITLE_CHARS),
                values = listOf(
                    Bars.Data(
                        value = manga.chapterCount.toDouble(),
                        // Phân cấp theo rank: top 1 đậm nhất, giảm dần độ đậm
                        color = SolidColor(
                            primaryColor.copy(
                                alpha = 1f - index * RANK_ALPHA_STEP,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    // Cao theo số truyện + trục X — hàng gọn để bar không cách xa nhau
    val rowChartHeight = (items.size * ROW_HEIGHT_DP + AXIS_HEIGHT_DP).dp
    ChartCard {
        RowChart(
            modifier = Modifier.fillMaxWidth().height(rowChartHeight).padding(Dimens.SpacingSm),
            data = rowData,
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, bottomRight = 6.dp),
                spacing = 4.dp,
                thickness = 20.dp,
            ),
            gridProperties = themedGridProperties(),
            labelProperties = themedLabelProperties(),
            labelHelperProperties = LabelHelperProperties(enabled = false),
            indicatorProperties = themedVerticalIndicatorProperties(
                maxValue = items.maxOf { it.chapterCount },
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
}

/**
 * Cắt title theo độ rộng ước lượng thay vì số ký tự thuần:
 * ký tự CJK (kanji/kana/hangul) rộng ~gấp đôi latin nên tính 2 đơn vị.
 * Tránh title tiếng Nhật/Trung bị clip trong label area của RowChart.
 */
private fun String.ellipsize(maxWidthUnits: Int): String {
    var width = 0
    forEachIndexed { index, char ->
        width += if (char.isCjk()) 2 else 1
        if (width > maxWidthUnits) {
            return take(index).trimEnd() + "…"
        }
    }
    return this
}

private fun Char.isCjk(): Boolean {
    val block = Character.UnicodeBlock.of(this) ?: return false
    return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
        block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
        block == Character.UnicodeBlock.HIRAGANA ||
        block == Character.UnicodeBlock.KATAKANA ||
        block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
        block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
}

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPaddingCompact)) {
            content()
        }
    }
}

private data class LegendItem(val color: Color, val label: String, val count: Int)

@Composable
private fun BreakdownLegend(items: List<LegendItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(Dimens.SpacingMd).clip(CircleShape).background(item.color))
                Spacer(Modifier.width(Dimens.SpacingSm))
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    item.count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun weekDayLabels(): List<String> {
    val today = LocalDate.now()
    return (0 until DAYS_IN_WEEK).map { offset ->
        val day = today.minusDays((DAYS_IN_WEEK - 1 - offset).toLong())
        day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}

@Composable
private fun themedGridProperties(): GridProperties {
    val gridColor = SolidColor(MaterialTheme.colorScheme.outlineVariant)
    return GridProperties(
        xAxisProperties = GridProperties.AxisProperties(color = gridColor),
        yAxisProperties = GridProperties.AxisProperties(color = gridColor),
    )
}

@Composable
private fun themedLabelProperties(): LabelProperties {
    val style = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return LabelProperties(enabled = true, textStyle = style)
}

@Composable
private fun themedLabelHelperProperties(): LabelHelperProperties {
    val style = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    return LabelHelperProperties(enabled = true, textStyle = style)
}

/**
 * Số tick sao cho mọi tick là số nguyên: max nhỏ (<4) thì mỗi bậc 1 tick,
 * tránh tick lẻ (0.25, 0.5…) bị toInt() cắt thành nhiều số 0 trùng nhau.
 */
private fun integerIndicatorCount(maxValue: Int): IndicatorCount =
    IndicatorCount.CountBased(count = minOf(maxValue, DEFAULT_INDICATOR_STEPS) + 1)

@Composable
private fun themedIndicatorProperties(maxValue: Int): HorizontalIndicatorProperties {
    val style = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Số chương luôn nguyên — không hiện tick thập phân
    return HorizontalIndicatorProperties(
        enabled = true,
        textStyle = style,
        count = integerIndicatorCount(maxValue),
        contentBuilder = { value -> value.roundToInt().toString() },
    )
}

@Composable
private fun themedVerticalIndicatorProperties(maxValue: Int): VerticalIndicatorProperties {
    val style = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return VerticalIndicatorProperties(
        enabled = true,
        textStyle = style,
        count = integerIndicatorCount(maxValue),
        contentBuilder = { value -> value.roundToInt().toString() },
    )
}

private const val DAYS_IN_WEEK = 7
private const val MAX_TITLE_CHARS = 20
private const val SUMMARY_VALUE_MAX_DIGITS = 4
private const val DEFAULT_INDICATOR_STEPS = 4
private const val ROW_HEIGHT_DP = 36
private const val AXIS_HEIGHT_DP = 48

/** Mỗi bậc rank giảm 15% độ đậm — top 1 đậm nhất (alpha 1.0 → 0.4 cho hạng 5). */
private const val RANK_ALPHA_STEP = 0.15f
