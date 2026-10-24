package com.ferbotz.momentic.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** The three shadow levels from the canvas — all cast in rose-deep at low opacity. */
enum class MomenticElevation {
    /** Cards and buttons. */
    Rest,

    /** The featured card and anything floating over content. */
    Raised,

    /** Bottom sheets and dialogs (cast upwards). */
    Sheet,
}

/**
 * Draws [elevation]'s shadow behind a [shape]d element. Put it before `background`/`clip` in the chain,
 * so the shadow isn't clipped away.
 */
@Composable
fun Modifier.momenticShadow(elevation: MomenticElevation, shape: Shape): Modifier =
    momenticShadow(elevation, shape, Momentic.colors.shadow)

fun Modifier.momenticShadow(elevation: MomenticElevation, shape: Shape, tint: Color): Modifier = when (elevation) {
    MomenticElevation.Rest -> this
        .dropShadow(shape, Shadow(radius = 2.dp, color = tint, offset = DpOffset(0.dp, 1.dp), alpha = 0.06f))
        .dropShadow(shape, Shadow(radius = 16.dp, color = tint, offset = DpOffset(0.dp, 4.dp), alpha = 0.06f))
    MomenticElevation.Raised -> this
        .dropShadow(shape, Shadow(radius = 6.dp, color = tint, offset = DpOffset(0.dp, 2.dp), alpha = 0.08f))
        .dropShadow(shape, Shadow(radius = 32.dp, color = tint, offset = DpOffset(0.dp, 12.dp), alpha = 0.10f))
    MomenticElevation.Sheet -> this
        .dropShadow(shape, Shadow(radius = 40.dp, color = tint, offset = DpOffset(0.dp, (-8).dp), alpha = 0.14f))
}
