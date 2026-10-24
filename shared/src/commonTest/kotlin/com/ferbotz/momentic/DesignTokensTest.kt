package com.ferbotz.momentic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ferbotz.momentic.theme.MomenticColors
import com.ferbotz.momentic.theme.occasionTint
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins the design canvas's contrast rules: every pairing the components draw text with meets WCAG AA
 * (4.5:1 for normal-size text). A token change that breaks one fails here, not on someone's phone.
 */
class DesignTokensTest {

    private val c = MomenticColors()

    @Test
    fun bodyTextIsInkOrInkSoftAndReadsOnEveryPaper() {
        listOf(c.background, c.surface, c.blush100, c.blush200, c.petal, c.petalPressed).forEach { paper ->
            assertAa(c.ink, paper, "ink on $paper")
        }
        listOf(c.background, c.surface, c.endedFill, c.line).forEach { paper ->
            assertAa(c.inkSoft, paper, "ink-soft on $paper")
        }
    }

    @Test
    fun primaryButtonsCarryAnInkLabelOnPetal() {
        assertAa(c.ink, c.petal, "button label")
        assertAa(c.ink, c.petalPressed, "pressed button label")
        assertAa(c.inkSoft, c.line, "disabled button label")
    }

    @Test
    fun roseTextReadsOnSurfaceAndBackgroundOnly() {
        assertAa(c.rose, c.surface, "rose link on surface")
        assertAa(c.rose, c.background, "rose link on background")
        assertAa(c.roseDeep, c.blush100, "pressed rose on blush")
        // The reason small text on blush is ink: rose there falls short.
        assertTrue(contrast(c.rose, c.blush100) < 4.5, "rose on blush-100 is expected to fail AA")
    }

    @Test
    fun whiteTextOnlySitsOnRoseDeep() {
        assertAa(c.white, c.roseDeep, "Premium badge")
    }

    @Test
    fun statusChipsReadInTheirOwnColour() {
        assertAa(c.success, c.successFill, "Live chip")
        assertAa(c.warning, c.warningFill, "Ending chip")
        assertAa(c.error, c.errorFill, "error banner")
        assertAa(c.error, c.surface, "destructive button label")
        assertAa(c.inkSoft, c.endedFill, "Ended chip")
        assertAa(c.success, c.background, "Saved note")
    }

    @Test
    fun eachOccasionAccentReadsOnItsOwnTint() {
        listOf("birthday", "wedding", "love", "anniversary", "friendship", "greeting", "unknown").forEach { id ->
            val tint = occasionTint(id)
            assertAa(tint.accent, tint.start, "$id accent on its light end")
            assertAa(tint.accent, tint.end, "$id accent on its deep end")
            assertAa(c.ink, tint.end, "ink on the $id tint")
        }
    }

    private fun assertAa(text: Color, background: Color, what: String) {
        val ratio = contrast(text, background)
        assertTrue(ratio >= 4.5, "$what is ${(ratio * 100).toInt() / 100.0}:1, below AA's 4.5:1")
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
