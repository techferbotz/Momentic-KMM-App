package com.ferbotz.momentic.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.ferbotz.momentic.data.media.pixelSize
import com.ferbotz.momentic.data.media.renderFramed
import com.ferbotz.momentic.data.media.scaledToFit
import com.ferbotz.momentic.data.media.toByteArray
import com.ferbotz.momentic.data.media.uiImageOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.UIKit.UIImageJPEGRepresentation

/** Drawn upright by UIKit (as the upload is), then handed to Skia as a JPEG. */
actual suspend fun decodePhotoPreview(bytes: ByteArray, maxDimension: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val image = uiImageOf(bytes) ?: return@runCatching null
        val (pixelWidth, pixelHeight) = image.pixelSize()
        if (pixelWidth <= 0 || pixelHeight <= 0) return@runCatching null
        val (width, height) = scaledToFit(pixelWidth, pixelHeight, maxDimension)
        val upright = renderFramed(image, pixelWidth, pixelHeight, turns = 0, left = 0, top = 0, scale = width.toDouble() / pixelWidth, width, height)
        val jpeg = UIImageJPEGRepresentation(upright, 0.9) ?: return@runCatching null
        Image.makeFromEncoded(jpeg.toByteArray()).toComposeImageBitmap()
    }.getOrNull()
}
