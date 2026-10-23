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
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

actual fun platformImageProcessor(): ImageProcessor = IosImageProcessor()

/**
 * UIKit: `UIImage` decodes (HEIC included) and knows its EXIF orientation; drawing it into a
 * `UIGraphicsImageRenderer` applies that orientation, crops and scales in one pass. The renderer is
 * safe off the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosImageProcessor : ImageProcessor {

    override suspend fun prepare(
        source: ByteArray,
        aspectRatio: AspectRatio?,
        maxBytes: Long,
        maxDimension: Int,
    ): AppResult<PreparedImage> = withContext(Dispatchers.IO) {
        if (source.isEmpty()) return@withContext AppResult.Failure(AppError.Invalid(UNREADABLE))
        val image = UIImage.imageWithData(source.toNSData())
            ?: return@withContext AppResult.Failure(AppError.Invalid(UNREADABLE))

        // `size` is in points and already accounts for orientation; scale converts to pixels.
        val (pointWidth, pointHeight) = image.size.useContents { width to height }
        val pixelWidth = (pointWidth * image.scale).toInt()
        val pixelHeight = (pointHeight * image.scale).toInt()
        if (pixelWidth <= 0 || pixelHeight <= 0) return@withContext AppResult.Failure(AppError.Invalid(UNREADABLE))

        val (left, top, cropWidth, cropHeight) = centreCrop(pixelWidth, pixelHeight, aspectRatio).toList()
        val (targetWidth, targetHeight) = scaledToFit(cropWidth, cropHeight, maxDimension)
        val scale = targetWidth.toDouble() / cropWidth.toDouble()

        val format = UIGraphicsImageRendererFormat.preferredFormat().apply { this.scale = 1.0 }
        val renderer = UIGraphicsImageRenderer(
            size = CGSizeMake(targetWidth.toDouble(), targetHeight.toDouble()),
            format = format,
        )
        val rendered = renderer.imageWithActions { _ ->
            // Draw the whole photo, shifted and scaled so only the crop rectangle lands in the canvas.
            image.drawInRect(CGRectMake(-left * scale, -top * scale, pixelWidth * scale, pixelHeight * scale))
        }

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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    return bytes
}
