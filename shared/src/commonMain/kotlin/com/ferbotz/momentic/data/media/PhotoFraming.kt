package com.ferbotz.momentic.data.media

import com.ferbotz.momentic.data.model.AspectRatio
import kotlin.math.roundToInt

/**
 * A rectangle inside a photo, as fractions of its width and height (0–1) — independent of the
 * resolution the crop screen happened to show it at.
 */
data class CropRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    init {
        require(width > 0f && height > 0f) { "A crop needs an area" }
    }
}

/**
 * How the person framed a photo on the crop screen, applied to the upright photo: first turned
 * [quarterTurns] × 90° clockwise, then cropped to [crop] (in the turned photo). A null [crop] keeps the
 * centred crop for the field's aspect ratio.
 */
data class PhotoFraming(val quarterTurns: Int = 0, val crop: CropRect? = null) {
    /** 0–3, however many turns were asked for. */
    val turns: Int get() = quarterTurns.mod(4)
}

/** A picked photo on its way to the form: the bytes as picked, and how it was framed (none: centred). */
class PickedPhoto(val bytes: ByteArray, val framing: PhotoFraming? = null)

/** The size of a [width] × [height] photo after [quarterTurns] clockwise quarter turns. */
fun turnedSize(width: Int, height: Int, quarterTurns: Int): Pair<Int, Int> =
    if (quarterTurns.mod(2) == 1) height to width else width to height

/**
 * The pixel rectangle to keep from a [width] × [height] photo (already turned), as
 * `(left, top, cropWidth, cropHeight)`: the person's [crop], or — without one — the centred crop for
 * [aspectRatio]. A crop that strays past the edges is pulled back inside.
 */
fun framedCrop(width: Int, height: Int, crop: CropRect?, aspectRatio: AspectRatio?): IntArray {
    if (crop == null || width <= 0 || height <= 0) return centreCrop(width, height, aspectRatio)
    val cropWidth = (crop.width * width).roundToInt().coerceIn(1, width)
    val cropHeight = (crop.height * height).roundToInt().coerceIn(1, height)
    val left = (crop.left * width).roundToInt().coerceIn(0, width - cropWidth)
    val top = (crop.top * height).roundToInt().coerceIn(0, height - cropHeight)
    return intArrayOf(left, top, cropWidth, cropHeight)
}
