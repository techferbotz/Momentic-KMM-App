package com.ferbotz.momentic.data.media

import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.PreparedImage
import com.ferbotz.momentic.data.result.AppResult

/**
 * Turns a picked photo into what `POST /images` should receive (`API.md`: "crop before upload"):
 *
 * 1. decoded and turned **upright** (camera photos carry their rotation in EXIF, which the server strips);
 * 2. turned and **cropped** as the person framed it on the crop screen ([framing]) — or, without that,
 *    **centre-cropped** to the field's [AspectRatio] when it has one (the template lays the photo out at
 *    that ratio);
 * 3. **scaled down** so the long edge is at most [maxDimension] (the server keeps ≤ 1280 px anyway);
 * 4. re-encoded as **JPEG**, lowering quality until it fits [maxBytes].
 *
 * HEIC/HEIF from iPhones comes out as JPEG too. Runs off the main thread.
 */
interface ImageProcessor {

    suspend fun prepare(
        source: ByteArray,
        aspectRatio: AspectRatio?,
        maxBytes: Long,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        framing: PhotoFraming? = null,
    ): AppResult<PreparedImage>

    companion object {
        /** Long-edge cap: room above the server's 1280 px output, small enough to upload quickly. */
        const val DEFAULT_MAX_DIMENSION: Int = 2048
    }
}

/** The platform's [ImageProcessor]: `BitmapFactory` on Android, UIKit on iOS. */
expect fun platformImageProcessor(): ImageProcessor

/**
 * The centred crop rectangle for [aspectRatio] inside a [width] × [height] image, as
 * `(left, top, cropWidth, cropHeight)`. Shared so both platforms crop identically.
 */
fun centreCrop(width: Int, height: Int, aspectRatio: AspectRatio?): IntArray {
    if (aspectRatio == null || width <= 0 || height <= 0) return intArrayOf(0, 0, width, height)
    val target = aspectRatio.ratio
    val current = width.toFloat() / height.toFloat()
    return if (current > target) {
        // Too wide: keep the full height, trim the sides.
        val cropWidth = (height * target).toInt().coerceIn(1, width)
        intArrayOf((width - cropWidth) / 2, 0, cropWidth, height)
    } else {
        // Too tall: keep the full width, trim top and bottom.
        val cropHeight = (width / target).toInt().coerceIn(1, height)
        intArrayOf(0, (height - cropHeight) / 2, width, cropHeight)
    }
}

/** The size that fits [width] × [height] inside [maxDimension] on its long edge, never upscaling. */
fun scaledToFit(width: Int, height: Int, maxDimension: Int): Pair<Int, Int> {
    val longEdge = maxOf(width, height)
    if (longEdge <= maxDimension) return width to height
    val scale = maxDimension.toFloat() / longEdge
    return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
}

/** JPEG qualities to try, best first, until the encoded photo fits under the byte limit. */
internal val JPEG_QUALITY_STEPS: List<Int> = listOf(88, 80, 72, 64, 56, 48)
