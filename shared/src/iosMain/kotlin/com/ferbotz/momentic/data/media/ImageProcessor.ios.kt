package com.ferbotz.momentic.data.media

import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.PreparedImage
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.PI

actual fun platformImageProcessor(): ImageProcessor = IosImageProcessor()

/**
 * UIKit: `UIImage` decodes (HEIC included) and knows its EXIF orientation; drawing it into a
 * `UIGraphicsImageRenderer` applies that orientation, and the context's transform turns, crops and
 * scales in the same pass. The renderer is safe off the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosImageProcessor : ImageProcessor {

    override suspend fun prepare(
        source: ByteArray,
        aspectRatio: AspectRatio?,
        maxBytes: Long,
        maxDimension: Int,
        framing: PhotoFraming?,
    ): AppResult<PreparedImage> = withContext(Dispatchers.IO) {
        val image = uiImageOf(source) ?: return@withContext AppResult.Failure(AppError.Invalid(UNREADABLE))
        val (pixelWidth, pixelHeight) = image.pixelSize()
        if (pixelWidth <= 0 || pixelHeight <= 0) return@withContext AppResult.Failure(AppError.Invalid(UNREADABLE))

        val turns = framing?.turns ?: 0
        val (turnedWidth, turnedHeight) = turnedSize(pixelWidth, pixelHeight, turns)
        val (left, top, cropWidth, cropHeight) = framedCrop(turnedWidth, turnedHeight, framing?.crop, aspectRatio).toList()
        val (targetWidth, targetHeight) = scaledToFit(cropWidth, cropHeight, maxDimension)
        val rendered = renderFramed(image, pixelWidth, pixelHeight, turns, left, top, targetWidth.toDouble() / cropWidth, targetWidth, targetHeight)

        for (quality in JPEG_QUALITY_STEPS) {
            val data = UIImageJPEGRepresentation(rendered, quality / 100.0) ?: continue
            if (data.length.toLong() <= maxBytes) {
                return@withContext AppResult.Success(PreparedImage(data.toByteArray(), "image/jpeg", targetWidth, targetHeight))
            }
        }
        AppResult.Failure(AppError.Invalid("That photo is too big to use, even shrunk. Try another one."))
    }

    private companion object {
        const val UNREADABLE = "That photo couldn't be opened. Try another one."
    }
}

/** The picked bytes as a `UIImage`, or null when they aren't a photo UIKit can read. */
internal fun uiImageOf(source: ByteArray): UIImage? = if (source.isEmpty()) null else UIImage.imageWithData(source.toNSData())

/** The upright size in pixels: `size` is in points and already accounts for the EXIF orientation. */
@OptIn(ExperimentalForeignApi::class)
internal fun UIImage.pixelSize(): Pair<Int, Int> {
    val (pointWidth, pointHeight) = size.useContents { width to height }
    return (pointWidth * scale).toInt() to (pointHeight * scale).toInt()
}

/**
 * Draws [image] (upright, [pixelWidth] × [pixelHeight]) turned [turns] quarter turns clockwise, with the
 * point ([left], [top]) of the turned photo at the origin, scaled by [scale], into a [targetWidth] ×
 * [targetHeight] image. UIKit's context runs y-down, so a positive rotation turns clockwise.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun renderFramed(
    image: UIImage,
    pixelWidth: Int,
    pixelHeight: Int,
    turns: Int,
    left: Int,
    top: Int,
    scale: Double,
    targetWidth: Int,
    targetHeight: Int,
): UIImage {
    val format = UIGraphicsImageRendererFormat.preferredFormat().apply { this.scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth.toDouble(), targetHeight.toDouble()), format = format)
    return renderer.imageWithActions { context ->
        val cg = context?.CGContext
        // Applied to the drawing in reverse: turn the upright photo, move the crop to the origin, scale.
        CGContextScaleCTM(cg, scale, scale)
        CGContextTranslateCTM(cg, -left.toDouble(), -top.toDouble())
        when (turns) {
            1 -> {
                CGContextTranslateCTM(cg, pixelHeight.toDouble(), 0.0)
                CGContextRotateCTM(cg, PI / 2)
            }
            2 -> {
                CGContextTranslateCTM(cg, pixelWidth.toDouble(), pixelHeight.toDouble())
                CGContextRotateCTM(cg, PI)
            }
            3 -> {
                CGContextTranslateCTM(cg, 0.0, pixelWidth.toDouble())
                CGContextRotateCTM(cg, -PI / 2)
            }
        }
        image.drawInRect(CGRectMake(0.0, 0.0, pixelWidth.toDouble(), pixelHeight.toDouble()))
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    return bytes
}
