package com.ferbotz.momentic.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * The palette from the design canvas (Tokens & components → Colour): light pink and cream, rose for the
 * things you tap, ink for everything you read. The web pages recipients open use the same palette, so
 * the app and the moments it sends feel like one brand. Light theme only.
 *
 * Contrast rules the tokens are chosen for (pinned by ThemeContrastTest):
 *  - body text is ink or ink-soft, never pink;
 *  - white text only on rose-deep (badges); primary buttons are petal with an ink label;
 *  - rose text is fine on surface and background; on blush fills and occasion tints small text is ink.
 */

/** Every colour the UI draws with. Read it through [Momentic.colors]. */
@Immutable
data class MomenticColors(
    /** App background — a very light pink. */
    val background: Color = Color(0xFFFDF4F5),
    /** Cards, sheets, inputs — warm cream paper. */
    val surface: Color = Color(0xFFFDF8F1),
    /** Hero areas, selected chips, soft fills. Small text on it is [ink]. */
    val blush100: Color = Color(0xFFF6DFE2),
    /** Gradients, illustrations, progress tracks. */
    val blush200: Color = Color(0xFFEFC9D2),
    /** Primary buttons, with an [ink] label. */
    val petal: Color = Color(0xFFF2AFBC),
    val petalPressed: Color = Color(0xFFE69CAB),
    /** Links, text buttons, the active tab, focus and selection. */
    val rose: Color = Color(0xFFB84D66),
    /** Pressed text actions, badges with white text, emphasis in headings. */
    val roseDeep: Color = Color(0xFF8A2F47),
    /** Outlines of secondary buttons and add-photo slots — decoration; the label carries the contrast. */
    val roseSoft: Color = Color(0xFFD68D9A),
    /** All body text and titles. */
    val ink: Color = Color(0xFF2F2521),
    /** Secondary text, helper text, captions. */
    val inkSoft: Color = Color(0xFF6D5C53),
    /** Borders and dividers; also the disabled fill. Never text. */
    val line: Color = Color(0xFFEAD7DB),
    /** "Live", uploaded, saved. */
    val success: Color = Color(0xFF2F6B52),
    val successFill: Color = Color(0xFFE4F0E9),
    /** "Ending soon". */
    val warning: Color = Color(0xFF7A5214),
    val warningFill: Color = Color(0xFFFBEFD9),
    /** Errors — always with an icon and words, never colour alone. */
    val error: Color = Color(0xFFA3242E),
    val errorFill: Color = Color(0xFFFBE9EA),
    /** Outline of destructive buttons. */
    val errorSoft: Color = Color(0xFFDC8B88),
    /** The "Ended" chip. */
    val endedFill: Color = Color(0xFFEFE8E4),
    /** Ink at 40% — behind sheets and dialogs. */
    val scrim: Color = Color(0x662F2521),
    /** The tint every shadow is cast in. */
    val shadow: Color = Color(0xFF8A2F47),
    /** Only for text and icons on [roseDeep], and the paper behind a QR code. */
    val white: Color = Color.White,
)

/**
 * A category's soft gradient and its accent — chips, section headers, art placeholders. The accent
 * reads as text on its own tint (AA), but small text on a tint stays [MomenticColors.ink].
 */
@Immutable
data class OccasionTint(
    val start: Color,
    val end: Color,
    val accent: Color,
) {
    /** The tint as a wash from [start] (top left) to [end] (bottom right). */
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
}

/** The six occasions, keyed by the feed's category id; anything else gets the greeting blush. */
fun occasionTint(categoryId: String?): OccasionTint = when (categoryId) {
    "birthday" -> OccasionTint(Color(0xFFF9EAD0), Color(0xFFF0D5A5), Color(0xFF7A5214))
    "wedding" -> OccasionTint(Color(0xFFE2E7EE), Color(0xFFC7D2E1), Color(0xFF384860))
    "love" -> OccasionTint(Color(0xFFF7E1E4), Color(0xFFEEC7D0), Color(0xFF8A2F47))
    "anniversary" -> OccasionTint(Color(0xFFE9E3F2), Color(0xFFD4C9E8), Color(0xFF47376A))
    "friendship" -> OccasionTint(Color(0xFFDDEEE1), Color(0xFFC0DDCE), Color(0xFF21503D))
    else -> OccasionTint(Color(0xFFF6DFE2), Color(0xFFEFC9D2), Color(0xFF8A2F47))
}

internal val LocalMomenticColors = staticCompositionLocalOf { MomenticColors() }

/**
 * The tokens mapped onto Material 3, for the Material components the app still uses as-is (text
 * selection, date and time pickers, the sheet's drag behaviour). Momentic components read
 * [MomenticColors] directly.
 */
internal fun MomenticColors.toMaterialColorScheme(): ColorScheme = lightColorScheme(
    primary = rose,
    onPrimary = white,
    primaryContainer = blush100,
    onPrimaryContainer = ink,
    inversePrimary = petal,
    secondary = roseDeep,
    onSecondary = white,
    secondaryContainer = petal,
    onSecondaryContainer = ink,
    tertiary = success,
    onTertiary = white,
    tertiaryContainer = successFill,
    onTertiaryContainer = success,
    background = background,
    onBackground = ink,
    surface = surface,
    onSurface = ink,
    surfaceVariant = blush100,
    onSurfaceVariant = inkSoft,
    surfaceTint = surface,
    inverseSurface = ink,
    inverseOnSurface = surface,
    error = error,
    onError = white,
    errorContainer = errorFill,
    onErrorContainer = error,
    outline = inkSoft,
    outlineVariant = line,
    scrim = scrim,
    surfaceBright = surface,
    surfaceDim = background,
    surfaceContainerLowest = surface,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = blush100,
)
