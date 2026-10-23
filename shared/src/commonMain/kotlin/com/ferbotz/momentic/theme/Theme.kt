package com.ferbotz.momentic.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Generous rounding throughout; template art uses `large`, pills use `CircleShape` directly.
 *
 *  - extraSmall 8 · small 12 — chips, small thumbnails
 *  - medium 20 — inputs, cards
 *  - large 28 — template art, sheets
 *  - extraLarge 36 — the largest containers
 */
private val MomenticShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val LocalMomenticDarkTheme = staticCompositionLocalOf { false }

/** The app theme: light and dark, following the device. */
@Composable
fun MomenticTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMomenticDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) MomenticDarkColorScheme else MomenticLightColorScheme,
            typography = momenticTypography(),
            shapes = MomenticShapes,
            content = content,
        )
    }
}

object MomenticTheme {
    /**
     * Whether the dark palette is being drawn — for surfaces the app hands to the platform (the in-app
     * browser), so they open in the same light or dark as the screen they came from.
     */
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalMomenticDarkTheme.current
}
