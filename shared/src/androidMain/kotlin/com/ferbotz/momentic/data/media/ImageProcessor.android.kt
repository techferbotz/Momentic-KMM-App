package com.ferbotz.momentic.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.PreparedImage
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual fun platformImageProcessor(): ImageProcessor = AndroidImageProcessor()

/** `BitmapFactory` + `ExifInterface`: sample-decoded, rotated upright, cropped, scaled, JPEG-encoded. */
private class AndroidImageProcessor : ImageProcessor {

    override suspend fun prepare(
        source: ByteArray,
        aspectRatio: AspectRatio?,
        maxBytes: Long,
        maxDimension: Int,
    ): AppResult<PreparedImage> = withContext(Dispatchers.Default) {
        runCatching { process(source, aspectRatio, maxBytes, maxDimension) }
            .getOrElse { AppResult.Failure(AppError.Invalid(UNREADABLE)) }
    }

    private fun process(source: ByteArray, aspectRatio: AspectRatio?, maxBytes: Long, maxDimension: Int): AppResult<PreparedImage> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return AppResult.Failure(AppError.Invalid(UNREADABLE))

        // Decode at the smallest power-of-two sample that still leaves the long edge ≥ maxDimension,
        // so a 48 MP photo never lands in memory at full size.
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            source, 0, source.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return AppResult.Failure(AppError.Invalid(UNREADABLE))

        val upright = decoded.rotatedUpright(exifOrientation(source))
        val (left, top, cropWidth, cropHeight) = centreCrop(upright.width, upright.height, aspectRatio).toList()
        val cropped = if (cropWidth == upright.width && cropHeight == upright.height) {
            upright
        } else {
            Bitmap.createBitmap(upright, left, top, cropWidth, cropHeight)
        }
        val (targetWidth, targetHeight) = scaledToFit(cropped.width, cropped.height, maxDimension)
        val scaled = if (targetWidth == cropped.width && targetHeight == cropped.height) {
            cropped
        } else {
            Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        }

        for (quality in JPEG_QUALITY_STEPS) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes) {
                return AppResult.Success(PreparedImage(bytes, "image/jpeg", scaled.width, scaled.height))
            }
        }
        return AppResult.Failure(AppError.Invalid("That photo is too big to use, even shrunk. Try another one."))
    }

    private fun exifOrientation(source: ByteArray): Int =
        runCatching {
            ExifInterface(ByteArrayInputStream(source))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun Bitmap.rotatedUpright(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private companion object {
        const val UNREADABLE = "That photo couldn't be opened. Try another one."
    }
}
