package com.ferbotz.momentic.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A picked photo for the crop screen: upright (its EXIF orientation applied, as the upload will be) and
 * at most [maxDimension] on its long edge. Null when it can't be read. Runs off the main thread.
 */
expect suspend fun decodePhotoPreview(bytes: ByteArray, maxDimension: Int = 1600): ImageBitmap?
