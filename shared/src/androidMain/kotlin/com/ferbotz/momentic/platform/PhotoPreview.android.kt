package com.ferbotz.momentic.platform

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ferbotz.momentic.data.media.decodeUpright
import com.ferbotz.momentic.data.media.scaledToFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun decodePhotoPreview(bytes: ByteArray, maxDimension: Int): ImageBitmap? = withContext(Dispatchers.Default) {
    runCatching {
        val upright = decodeUpright(bytes, maxDimension) ?: return@runCatching null
        val (width, height) = scaledToFit(upright.width, upright.height, maxDimension)
        val sized = if (width == upright.width && height == upright.height) upright else Bitmap.createScaledBitmap(upright, width, height, true)
        sized.asImageBitmap()
    }.getOrNull()
}
