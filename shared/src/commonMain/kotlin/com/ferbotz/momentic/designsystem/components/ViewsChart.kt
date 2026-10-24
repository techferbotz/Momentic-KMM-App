package com.ferbotz.momentic.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.DailyViews
import com.ferbotz.momentic.presentation.util.dayMonthLabel
import com.ferbotz.momentic.presentation.util.shortLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.chart_summary
import com.ferbotz.momentic.resources.chart_today
import com.ferbotz.momentic.resources.chart_tooltip
import com.ferbotz.momentic.resources.views_count
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val MinAxisLabelWidth = 22.dp
private val AxisGap = 8.dp
private val MinPlotTop = 32.dp
private val PlotHeight = 120.dp
private val BarGap = 2.dp
private val MaxBarWidth = 24.dp
private val LabelGap = 6.dp
private val TooltipPaddingX = 8.dp
private val TooltipPaddingY = 4.dp
private val TooltipLift = 8.dp
private val TooltipShape = RoundedCornerShape(10.dp)
private val PeakLift = 2.dp
private val PeakKnockout = 3.dp

/**
 * A moment's views per day (the backend sends the last 30): thin rose columns with 4 dp rounded tops on a
 * hairline grid, the busiest day labelled on its cap, and the chosen day — today, until a tap or a drag
 * picks another — in rose-deep. Its tooltip floats above the tallest bar it would cover, a hairline stem
 * down to the day, so it never hides a bar. One series, so no legend; the screen's heading names it.
 * Every label is sized from its text, so the chart keeps its shape at any font size.
 */
@Composable
fun ViewsBarChart(days: List<DailyViews>, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    val c = Momentic.colors
    val type = Momentic.type
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var selected by rememberSaveable(days.size) { mutableIntStateOf(days.lastIndex) }
    val axisMax = remember(days) { chartAxisMax(days.maxOf { it.views }) }
    val peak = remember(days) { days.indices.maxBy { days[it].views } }
    val today = stringResource(Res.string.chart_today)
    val selectedViews = days[selected].views.toInt()
    val tooltip = stringResource(
        Res.string.chart_tooltip,
        if (selected == days.lastIndex) today else days[selected].date?.shortLabel() ?: "${selected + 1}",
        pluralStringResource(Res.plurals.views_count, selectedViews, selectedViews),
    )
    val summary = stringResource(Res.string.chart_summary, days.size, days[peak].views.toInt(), days.last().views.toInt())

    fun measure(text: String, style: TextStyle): IntSize = measurer.measure(text, style, maxLines = 1, softWrap = false).size
    val axisLabels = listOf(axisMax, axisMax / 2, 0L).map(Long::toString)
    val peakLabel = days[peak].views.toString()
    val tooltipSize = measure(tooltip, type.captionStrong)
    val peakSize = measure(peakLabel, type.captionStrong)
    val axisWidth: Dp
    val captionLine: Dp
    val tooltipWidth: Dp
    val tooltipHeight: Dp
    val peakWidth: Dp
    val peakHeight: Dp
    with(density) {
        axisWidth = maxOf(MinAxisLabelWidth, axisLabels.maxOf { measure(it, type.caption).width }.toDp())
        captionLine = measure(today, type.caption).height.toDp()
        tooltipWidth = tooltipSize.width.toDp() + TooltipPaddingX * 2
        tooltipHeight = tooltipSize.height.toDp() + TooltipPaddingY * 2
        peakWidth = peakSize.width.toDp() + PeakKnockout * 2
        peakHeight = peakSize.height.toDp()
    }
    val plotStart = axisWidth + AxisGap
    // The plot starts below a tooltip's height, so a tooltip resting at the very top still clears every bar.
    val plotTop = maxOf(MinPlotTop, tooltipHeight + LabelGap)
    val chartHeight = plotTop + PlotHeight + LabelGap + captionLine

    BoxWithConstraints(modifier.fillMaxWidth().height(chartHeight).semantics { contentDescription = summary }) {
        val plotWidth = maxWidth - plotStart
        val slot = plotWidth / days.size
        val barWidth = minOf(slot - BarGap, MaxBarWidth)
        fun centreOf(index: Int): Dp = plotStart + slot * index + slot / 2
        fun topOf(index: Int): Dp = plotTop + PlotHeight * (1f - days[index].views.toFloat() / axisMax)

        val tooltipLeft = clampTooltipLeft(centreOf(selected).value, tooltipWidth.value, plotStart.value, maxWidth.value).dp
        val tooltipTop = tooltipTop(
            tops = days.indices.map { topOf(it).value },
            plotStart = plotStart.value,
            slot = slot.value,
            left = tooltipLeft.value,
            width = tooltipWidth.value,
            height = tooltipHeight.value,
            lift = TooltipLift.value,
        ).dp
        val peakLeft = centreOf(peak) - peakWidth / 2
        val peakTop = topOf(peak) - PeakLift - peakHeight
        val tooltipRect = Rect(Offset(tooltipLeft.value, tooltipTop.value), Size(tooltipWidth.value, tooltipHeight.value))
        val peakRect = Rect(Offset(peakLeft.value, peakTop.value), Size(peakWidth.value, peakHeight.value))
        val showPeak = peak != selected && days[peak].views > 0 && !peakRect.overlaps(tooltipRect)

        axisLabels.forEachIndexed { row, label ->
            Text(
                text = label,
                style = type.caption,
                color = c.inkSoft,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.width(axisWidth).offset(y = plotTop + PlotHeight * (row / 2f) - captionLine / 2),
            )
        }

        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val left = plotStart.toPx()
            val top = plotTop.toPx()
            val height = PlotHeight.toPx()
            val hairline = 1.dp.toPx()
            val grid: DrawScope.() -> Unit = {
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    drawRect(c.line, topLeft = Offset(left, top + height * fraction), size = Size(size.width - left, hairline))
                }
            }
            // The gridline the peak label sits on breaks around it rather than running through it.
            if (showPeak) {
                clipRect(peakLeft.toPx(), peakTop.toPx(), (peakLeft + peakWidth).toPx(), (peakTop + peakHeight).toPx(), ClipOp.Difference, grid)
            } else {
                grid()
            }

            val stemX = centreOf(selected).toPx()
            drawLine(c.roseSoft, Offset(stemX, (tooltipTop + tooltipHeight).toPx()), Offset(stemX, topOf(selected).toPx()), strokeWidth = hairline)

            val slotPx = slot.toPx()
            val barPx = barWidth.toPx()
            val radius = CornerRadius(4.dp.toPx())
            days.indices.forEach { index ->
                val barTop = topOf(index).toPx()
                if (barTop >= top + height) return@forEach
                val x = left + slotPx * index + (slotPx - barPx) / 2
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = x,
                            top = barTop,
                            right = x + barPx,
                            bottom = top + height,
                            topLeftCornerRadius = radius,
                            topRightCornerRadius = radius,
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero,
                        ),
                    )
                }
                drawPath(path, if (index == selected) c.roseDeep else c.rose)
            }
        }

        // Tap or drag anywhere over the days to read one.
        Box(
            Modifier
                .offset(x = plotStart)
                .size(plotWidth, chartHeight)
                .pointerInput(days.size) {
                    detectTapGestures { position -> selected = indexAt(position.x, size.width.toFloat(), days.size) }
                }
                .pointerInput(days.size) {
                    detectHorizontalDragGestures { change, _ ->
                        selected = indexAt(change.position.x, size.width.toFloat(), days.size)
                    }
                },
        )

        if (showPeak) {
            Text(
                text = peakLabel,
                style = type.captionStrong,
                color = c.ink,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = peakLeft + PeakKnockout, y = peakTop),
            )
        }

        Text(
            text = tooltip,
            style = type.captionStrong,
            color = c.ink,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .offset(x = tooltipLeft, y = tooltipTop)
                .momenticShadow(MomenticElevation.Raised, TooltipShape)
                .clip(TooltipShape)
                .background(c.surface)
                .border(1.dp, c.line, TooltipShape)
                .padding(horizontal = TooltipPaddingX, vertical = TooltipPaddingY),
        )

        val labelTop = plotTop + PlotHeight + LabelGap
        Text(
            text = days.first().date?.dayMonthLabel().orEmpty(),
            style = type.caption,
            color = c.inkSoft,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(x = plotStart, y = labelTop),
        )
        if (days.size > 2) {
            val middle = days.size / 2
            Text(
                text = days[middle].date?.dayMonthLabel().orEmpty(),
                style = type.caption,
                color = c.inkSoft,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = centreOf(middle) - 28.dp, y = labelTop).width(56.dp).wrapContentWidth(unbounded = true),
            )
        }
        Text(
            text = today,
            style = type.caption,
            color = c.inkSoft,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.TopEnd).offset(y = labelTop),
        )
    }
}

/**
 * The top of the value axis: a clean number at or above [maxValue] whose half is a whole number too —
 * 46 → 50, 342 → 400, 13 → 20 — and never below 4, so a quiet week doesn't fill the chart.
 */
internal fun chartAxisMax(maxValue: Long): Long {
    if (maxValue <= 4) return 4
    if (maxValue <= 10) return if (maxValue % 2 == 0L) maxValue else maxValue + 1
    var magnitude = 1L
    while (magnitude * 10 < maxValue) magnitude *= 10
    for (step in longArrayOf(1, 2, 4, 5, 10)) {
        val candidate = step * magnitude
        if (candidate >= maxValue) return candidate
    }
    return 10 * magnitude
}

/** The tooltip's left edge: centred on its bar, but kept inside [minLeft]…[maxRight]. */
internal fun clampTooltipLeft(centre: Float, width: Float, minLeft: Float, maxRight: Float): Float =
    (centre - width / 2).coerceIn(minLeft, (maxRight - width).coerceAtLeast(minLeft))

/**
 * The tooltip's top edge: [lift] above the tallest bar under it, so it never hides one — and never above
 * the chart. [tops] are the bars' top edges (smaller is taller), in [slot]-wide columns from [plotStart].
 */
internal fun tooltipTop(tops: List<Float>, plotStart: Float, slot: Float, left: Float, width: Float, height: Float, lift: Float): Float {
    if (tops.isEmpty() || slot <= 0f) return 0f
    val first = ((left - plotStart) / slot).toInt().coerceIn(0, tops.lastIndex)
    val last = ((left + width - plotStart) / slot).toInt().coerceIn(0, tops.lastIndex)
    val highest = (first..last).minOf { tops[it] }
    return (highest - lift - height).coerceAtLeast(0f)
}

private fun indexAt(x: Float, width: Float, count: Int): Int =
    if (width <= 0f) count - 1 else ((x / width) * count).toInt().coerceIn(0, count - 1)
