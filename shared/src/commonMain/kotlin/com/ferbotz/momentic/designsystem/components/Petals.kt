package com.ferbotz.momentic.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticColors

/**
 * Soft petals drifting down — the app's one celebration, on "Your moment is live". Decorative only: it
 * draws nothing for screen readers and stands still when the system asks for reduced motion.
 */
@Composable
fun FallingPetals(modifier: Modifier = Modifier) {
    val colors = Momentic.colors
    val reduceMotion = Momentic.reduceMotion
    val petals = remember(colors) { petalsFor(colors) }
    var elapsed by remember { mutableLongStateOf(0L) }
    if (!reduceMotion) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) withFrameMillis { elapsed = it - start }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val travel = size.height + 48.dp.toPx()
        petals.forEach { petal ->
            val t = ((elapsed + petal.offsetMs) % petal.periodMs).toFloat() / petal.periodMs
            val fade = when {
                t < 0.1f -> t / 0.1f
                t > 0.85f -> (1f - t) / 0.15f
                else -> 1f
            }
            val width = petal.width.dp.toPx()
            val height = petal.height.dp.toPx()
            val x = petal.x * size.width + petal.drift.dp.toPx() * t
            val y = -24.dp.toPx() + travel * t
            val shape = Path().apply {
                moveTo(0f, height)
                quadraticTo(0f, 0f, width, 0f)
                quadraticTo(width, height, 0f, height)
                close()
            }
            translate(x, y) {
                rotate(petal.spin * t, pivot = Offset(width / 2, height / 2)) {
                    drawPath(shape, petal.color, alpha = petal.alpha * fade.coerceIn(0f, 1f))
                }
            }
        }
    }
}

private class Petal(
    val x: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val alpha: Float,
    val periodMs: Long,
    val offsetMs: Long,
    val drift: Float,
    val spin: Float,
)

private fun petalsFor(c: MomenticColors): List<Petal> = listOf(
    Petal(0.06f, 12f, 16f, c.blush200, 0.95f, 9_000, 1_000, 46f, 280f),
    Petal(0.18f, 9f, 12f, c.rose, 0.45f, 11_000, 6_000, 40f, 240f),
    Petal(0.30f, 13f, 17f, c.blush100, 0.95f, 8_500, 3_500, 50f, 300f),
    Petal(0.44f, 10f, 14f, c.blush200, 0.95f, 10_000, 8_000, 42f, 260f),
    Petal(0.57f, 12f, 15f, c.petal, 0.8f, 9_500, 2_000, 46f, 280f),
    Petal(0.68f, 9f, 13f, c.roseDeep, 0.35f, 12_000, 5_000, 38f, 220f),
    Petal(0.79f, 13f, 17f, c.blush200, 0.95f, 8_000, 4_200, 48f, 300f),
    Petal(0.90f, 10f, 14f, c.blush100, 0.95f, 10_500, 9_000, 40f, 260f),
    Petal(0.38f, 8f, 11f, c.rose, 0.4f, 13_000, 11_000, 36f, 240f),
    Petal(0.12f, 11f, 15f, c.petal, 0.8f, 11_500, 7_500, 44f, 270f),
)
