package com.ferbotz.momentic.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Momentic's palette: soft pink and warm neutrals (the brand direction the web templates share). The
 * app is a quiet frame around the user's photos and words, so colour is saved for the moments that
 * matter — the primary action, the price, the selected state.
 */

private val Rose = Color(0xFFB83B66)
private val RoseDeep = Color(0xFF3E0020)
private val Blush = Color(0xFFFFD9E2)
private val Berry = Color(0xFF8A4A5A)
private val BerryContainer = Color(0xFFFFD9DF)
private val Amber = Color(0xFF8F5530)
private val AmberContainer = Color(0xFFFFDCC6)

internal val MomenticLightColorScheme = lightColorScheme(
    primary = Rose,
    onPrimary = Color.White,
    primaryContainer = Blush,
    onPrimaryContainer = RoseDeep,
    secondary = Berry,
    onSecondary = Color.White,
    secondaryContainer = BerryContainer,
    onSecondaryContainer = Color(0xFF3A0718),
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Color(0xFF331200),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE1),
    onSurfaceVariant = Color(0xFF534346),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0F2),
    surfaceContainer = Color(0xFFFAEAEC),
    surfaceContainerHigh = Color(0xFFF4E4E6),
    surfaceContainerHighest = Color(0xFFEEDEE0),
    outline = Color(0xFF857376),
    outlineVariant = Color(0xFFD8C1C5),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF382E30),
    inverseOnSurface = Color(0xFFFDEDEF),
    inversePrimary = Color(0xFFFFB1C6),
    scrim = Color.Black,
)

internal val MomenticDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB1C6),
    onPrimary = Color(0xFF5E1131),
    primaryContainer = Color(0xFF7E2A4B),
    onPrimaryContainer = Blush,
    secondary = Color(0xFFF2B6C2),
    onSecondary = Color(0xFF52202E),
    secondaryContainer = Color(0xFF6D3644),
    onSecondaryContainer = BerryContainer,
    tertiary = Color(0xFFFFB787),
    onTertiary = Color(0xFF512300),
    tertiaryContainer = Color(0xFF713D1A),
    onTertiaryContainer = AmberContainer,
    background = Color(0xFF1A1113),
    onBackground = Color(0xFFF0DEE0),
    surface = Color(0xFF1A1113),
    onSurface = Color(0xFFF0DEE0),
    surfaceVariant = Color(0xFF534346),
    onSurfaceVariant = Color(0xFFD8C1C5),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainerLow = Color(0xFF22191B),
    surfaceContainer = Color(0xFF271D1F),
    surfaceContainerHigh = Color(0xFF32282A),
    surfaceContainerHighest = Color(0xFF3D3234),
    outline = Color(0xFFA08C90),
    outlineVariant = Color(0xFF534346),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF0DEE0),
    inverseOnSurface = Color(0xFF382E30),
    inversePrimary = Rose,
    scrim = Color.Black,
)

/**
 * Two-stop gradients standing in for a template's art while it loads or when it has none yet (five
 * of the six live templates have no thumbnail today). Keyed by category, so a card still reads as
 * "a birthday thing" before its picture arrives.
 */
internal fun categoryGradient(categoryId: String, dark: Boolean): List<Color> {
    val light = when (categoryId) {
        "birthday" -> listOf(Color(0xFFFFE1C7), Color(0xFFFFC2D4))
        "wedding" -> listOf(Color(0xFFF8EEDC), Color(0xFFF1D3DA))
        "love" -> listOf(Color(0xFFFFC9D6), Color(0xFFD9A7C7))
        "anniversary" -> listOf(Color(0xFFE9DDF6), Color(0xFFF6C9D6))
        "friendship" -> listOf(Color(0xFFD7F0E6), Color(0xFFD5E6FA))
        "greeting" -> listOf(Color(0xFFFFF0D9), Color(0xFFFBD5DC))
        else -> listOf(Color(0xFFFCE4EA), Color(0xFFF3D0DA))
    }
    return if (dark) light.map { it.copy(alpha = 0.22f).compositeOverDark() } else light
}

private fun Color.compositeOverDark(): Color {
    val base = Color(0xFF2A1F22)
    return Color(
        red = red * alpha + base.red * (1 - alpha),
        green = green * alpha + base.green * (1 - alpha),
        blue = blue * alpha + base.blue * (1 - alpha),
        alpha = 1f,
    )
}
