package com.ferbotz.momentic.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Display and headline styles are set in the platform serif — template names ("The Letter", "Our
 * Years") read like titles on a card, which is the feeling the web pages carry. Body and labels stay
 * in the platform sans for legibility in forms.
 */
internal fun momenticTypography(): Typography {
    val base = Typography()
    val serif = FontFamily.Serif
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = serif, fontWeight = FontWeight.Normal),
        displayMedium = base.displayMedium.copy(fontFamily = serif, fontWeight = FontWeight.Normal),
        displaySmall = base.displaySmall.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Small caps-style overline for section labels ("WHAT YOU'LL ADD"). */
internal val OverlineStyle = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.2.sp,
)
