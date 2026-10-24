package com.ferbotz.momentic.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.lora
import com.ferbotz.momentic.resources.lora_italic
import com.ferbotz.momentic.resources.plus_jakarta_sans
import org.jetbrains.compose.resources.Font

/*
 * Lora — a warm, bookish serif that echoes the web pages — for headings and anything emotional;
 * Plus Jakarta Sans for everything else. Both ship as variable fonts in composeResources/font (OFL;
 * licences in composeResources/files/licenses). Each weight below is an instance of the one file.
 */

/** The type scale from the design canvas. Read it through [Momentic.type]. */
@Immutable
data class MomenticTypography(
    /** Lora 500 · 32/40 — tab titles, welcome, the success moment. */
    val display: TextStyle,
    /** Lora 500 · 26/32 — screen and sheet headings, empty states. */
    val h1: TextStyle,
    /** Lora 600 · 20/28 — top bars, section titles, dialogs. */
    val h2: TextStyle,
    /** Lora 600 · 16/24 — template and moment names on cards. */
    val cardTitle: TextStyle,
    /** Lora italic · 15/22 — people's own words quoted back (guest messages). */
    val quote: TextStyle,
    /** Jakarta 400 · 16/24 — everything people read. */
    val body: TextStyle,
    /** Jakarta 400 · 14/20 — supporting text in rows, cards and banners. */
    val bodySmall: TextStyle,
    /** Jakarta 600 · 16/24 — button labels. */
    val button: TextStyle,
    /** Jakarta 600 · 14/20 — field labels, chips, tabs, rows. */
    val label: TextStyle,
    /** Jakarta 400 · 12/16 — helper text, counters, meta. */
    val caption: TextStyle,
    /** Jakarta 600 · 12/16 — badges, status chips, the bottom bar. */
    val captionStrong: TextStyle,
    /** Jakarta 700 · 12/16, letter-spaced capitals — "FEATURED", section labels. */
    val eyebrow: TextStyle,
    /** Jakarta 700 · 48/56 — the one big number on a screen (total views). */
    val hero: TextStyle,
)

@Composable
private fun loraFamily(): FontFamily = FontFamily(
    Font(Res.font.lora, FontWeight.Normal),
    Font(Res.font.lora, FontWeight.Medium),
    Font(Res.font.lora, FontWeight.SemiBold),
    Font(Res.font.lora, FontWeight.Bold),
    Font(Res.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.lora_italic, FontWeight.Medium, FontStyle.Italic),
    Font(Res.font.lora_italic, FontWeight.SemiBold, FontStyle.Italic),
)

@Composable
private fun jakartaFamily(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans, FontWeight.Normal),
    Font(Res.font.plus_jakarta_sans, FontWeight.Medium),
    Font(Res.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans, FontWeight.Bold),
    Font(Res.font.plus_jakarta_sans, FontWeight.ExtraBold),
)

@Composable
internal fun momenticTypography(): MomenticTypography {
    val serif = loraFamily()
    val sans = jakartaFamily()
    return remember(serif, sans) { buildTypography(serif, sans) }
}

private fun buildTypography(serif: FontFamily, sans: FontFamily): MomenticTypography =
    MomenticTypography(
        display = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
        h1 = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 32.sp),
        h2 = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
        cardTitle = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        quote = TextStyle(fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 22.sp),
        body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        button = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        label = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        caption = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        captionStrong = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
        eyebrow = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.08.em,
        ),
        hero = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.01).em),
    )

internal val LocalMomenticTypography = staticCompositionLocalOf<MomenticTypography> {
    error("MomenticTypography is only available inside MomenticTheme")
}

/** The scale mapped onto Material 3, for the Material components used as they are. */
internal fun MomenticTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = h1,
    headlineMedium = h1,
    headlineSmall = h1,
    titleLarge = h2,
    titleMedium = cardTitle,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = bodySmall,
    bodySmall = caption,
    labelLarge = button,
    labelMedium = label,
    labelSmall = captionStrong,
)
