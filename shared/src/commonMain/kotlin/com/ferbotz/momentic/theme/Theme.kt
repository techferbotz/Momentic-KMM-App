package com.ferbotz.momentic.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import com.ferbotz.momentic.platform.rememberReduceMotion

/**
 * The app theme — the design canvas's tokens, light only. Momentic components read [Momentic]; the
 * Material components still in use get the same tokens through [MaterialTheme].
 */
@Composable
fun MomenticTheme(content: @Composable () -> Unit) {
    val colors = remember { MomenticColors() }
    val typography = momenticTypography()
    val reduceMotion = rememberReduceMotion()
    CompositionLocalProvider(
        LocalMomenticColors provides colors,
        LocalMomenticTypography provides typography,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = remember(colors) { colors.toMaterialColorScheme() },
            typography = remember(typography) { typography.toMaterialTypography() },
            shapes = MomenticShapes.material,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.ink,
                LocalTextStyle provides typography.body,
                content = content,
            )
        }
    }
}

/** The current theme's tokens. */
object Momentic {
    val colors: MomenticColors
        @Composable @ReadOnlyComposable
        get() = LocalMomenticColors.current

    val type: MomenticTypography
        @Composable @ReadOnlyComposable
        get() = LocalMomenticTypography.current

    /** The system asks for reduced motion: loops (petals, pulses) stand still. */
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
}
