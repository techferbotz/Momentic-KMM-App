package com.ferbotz.momentic.designsystem.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** The Momentic mark over the middle of the code: a white tile with the rose-deep "m". Small enough for Quartile correction to read through. */
private const val MARK_FRACTION = 0.19f

/**
 * [code] drawn square: [dark] modules on [light], no quiet zone (the screen's white card is the margin),
 * and the Momentic mark in the middle when [mark] is given. [description] is what a screen reader says.
 */
@Composable
fun QrCodeImage(
    code: QrCode,
    description: String,
    modifier: Modifier = Modifier,
    dark: Color = Color(0xFF2F2521),
    light: Color = Color.White,
    mark: QrMark? = null,
) {
    Canvas(modifier.aspectRatio(1f).semantics { contentDescription = description }) {
        drawQr(code, light, dark, quietZone = 0, mark = mark)
    }
}

/** The mark drawn over the code's middle: [letter] in [ink] on a [circle] disc, on a [tile]. */
class QrMark(
    val letter: String,
    val ink: Color,
    val circle: Color,
    val tile: Color,
    val measurer: TextMeasurer,
    val style: TextStyle,
)

/** [code] as a bitmap [sizePx] square, with a four-module quiet zone — what "Save image" writes. */
fun qrBitmap(code: QrCode, sizePx: Int, dark: Color, light: Color, mark: QrMark?): ImageBitmap {
    val bitmap = ImageBitmap(sizePx, sizePx)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(sizePx.toFloat(), sizePx.toFloat())) {
        drawQr(code, light, dark, quietZone = 4, mark = mark)
    }
    return bitmap
}

private fun DrawScope.drawQr(code: QrCode, light: Color, dark: Color, quietZone: Int, mark: QrMark?) {
    drawRect(light)
    val modules = code.size + quietZone * 2
    val module = size.minDimension / modules
    val origin = (size.minDimension - module * modules) / 2 + module * quietZone
    // Each dark module, slightly overlapped with its neighbours so no hairline seams show.
    for (y in 0 until code.size) {
        for (x in 0 until code.size) {
            if (code[x, y]) {
                drawRect(dark, Offset(origin + x * module, origin + y * module), Size(module + 0.6f, module + 0.6f))
            }
        }
    }
    if (mark == null) return
    val codeSize = module * code.size
    val tile = codeSize * MARK_FRACTION
    val tileOffset = Offset(origin + (codeSize - tile) / 2, origin + (codeSize - tile) / 2)
    drawRoundRect(mark.tile, tileOffset, Size(tile, tile), CornerRadius(tile * 0.25f))
    val radius = tile * 0.375f
    val centre = Offset(tileOffset.x + tile / 2, tileOffset.y + tile / 2)
    drawCircle(mark.circle, radius, centre)
    // Measured at this scope's density — the screen's, or 1 for the saved bitmap.
    val letter = mark.measurer.measure(mark.letter, mark.style.copy(color = mark.ink, fontSize = (radius * 1.1f).toSp()), density = this)
    drawText(letter, topLeft = Offset(centre.x - letter.size.width / 2f, centre.y - letter.size.height / 2f))
}
