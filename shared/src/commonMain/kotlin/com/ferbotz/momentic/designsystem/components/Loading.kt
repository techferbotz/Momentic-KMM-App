package com.ferbotz.momentic.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_loading
import com.ferbotz.momentic.theme.Momentic
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** A small indeterminate spinner — inside loading buttons and the "Saving…" note. */
@Composable
fun Spinner(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp, strokeWidth: Dp = 2.dp) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = strokeWidth,
        trackColor = color.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
    )
}

/**
 * Skeleton blocks pulse together inside a group — blush-100 shapes of what's coming, one slow pulse
 * (still when the system asks for reduced motion). Screen readers hear "Loading" once.
 */
@Composable
fun SkeletonGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val label = stringResource(Res.string.cd_loading)
    val alpha = if (Momentic.reduceMotion) {
        1f
    } else {
        rememberInfiniteTransition(label = "skeleton")
            .animateFloat(
                initialValue = 1f,
                targetValue = 0.55f,
                animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse",
            ).value
    }
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        content()
    }
}

/** One skeleton shape. Size it with [modifier]. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier.clip(shape).background(Momentic.colors.blush100))
}

/**
 * An upload's progress over a photo: a cream disc with a rose arc on a blush track, and the percentage
 * in ink. A null [progress] spins instead (the size isn't known yet).
 */
@Composable
fun ProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 4.dp,
    showPercent: Boolean = true,
) {
    val c = Momentic.colors
    val label = stringResource(Res.string.cd_loading)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(c.surface)
            .semantics {
                contentDescription = label
                if (progress != null) progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (progress == null) {
            Spinner(color = c.rose, size = size - strokeWidth * 3, strokeWidth = strokeWidth)
        } else {
            val fraction = progress.coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val inset = stroke * 1.5f
                val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(c.blush200, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
                drawArc(
                    color = c.rose,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            if (showPercent) {
                Text("${(fraction * 100).roundToInt()}%", style = Momentic.type.captionStrong, color = c.ink)
            }
        }
    }
}
