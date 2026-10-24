package com.ferbotz.momentic

import com.ferbotz.momentic.designsystem.components.chartAxisMax
import com.ferbotz.momentic.designsystem.components.clampTooltipLeft
import com.ferbotz.momentic.designsystem.components.tooltipTop
import com.ferbotz.momentic.presentation.util.dayMonthLabel
import com.ferbotz.momentic.presentation.util.longLabel
import com.ferbotz.momentic.presentation.util.shortLabel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewsChartTest {

    @Test
    fun theAxisTopsOutAtACleanNumberWithAWholeHalf() {
        assertEquals(4L, chartAxisMax(0))
        assertEquals(4L, chartAxisMax(3))
        assertEquals(6L, chartAxisMax(5))
        assertEquals(10L, chartAxisMax(10))
        assertEquals(20L, chartAxisMax(11))
        assertEquals(20L, chartAxisMax(13))
        assertEquals(50L, chartAxisMax(46))
        assertEquals(100L, chartAxisMax(100))
        assertEquals(400L, chartAxisMax(342))
        assertEquals(2000L, chartAxisMax(1001))
    }

    @Test
    fun theTooltipStaysInsideThePlot() {
        // Plot from 30 to 310; a 124-wide tooltip.
        assertEquals(186f, clampTooltipLeft(centre = 306f, width = 124f, minLeft = 30f, maxRight = 310f))
        assertEquals(30f, clampTooltipLeft(centre = 43f, width = 124f, minLeft = 30f, maxRight = 310f))
        assertEquals(88f, clampTooltipLeft(centre = 150f, width = 124f, minLeft = 30f, maxRight = 310f))
    }

    @Test
    fun theTooltipFloatsAboveEveryBarItCovers() {
        // Five 10-wide columns from 0; tops are y values, so smaller is taller. A 24-high tooltip, lifted 8.
        val tops = listOf(100f, 50f, 80f, 90f, 10f)
        // Over columns 1–3 it clears the tallest of them, not just its own day.
        assertEquals(18f, tooltipTop(tops, plotStart = 0f, slot = 10f, left = 15f, width = 20f, height = 24f, lift = 8f))
        // No room above the tallest: it rests at the top of the chart.
        assertEquals(0f, tooltipTop(tops, plotStart = 0f, slot = 10f, left = 30f, width = 20f, height = 24f, lift = 8f))
    }

    @Test
    fun datesReadDayFirst() {
        assertEquals("Thu, 24 Sep", LocalDate(2026, 9, 24).shortLabel())
        assertEquals("8 Oct", LocalDate(2026, 10, 8).dayMonthLabel())
        assertEquals("30 October", LocalDate(2026, 10, 30).longLabel())
    }
}
