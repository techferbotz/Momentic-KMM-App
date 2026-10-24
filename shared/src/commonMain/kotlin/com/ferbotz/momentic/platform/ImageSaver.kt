package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Saves an image (a moment's QR code) to the photo library, asking for permission where the platform needs it. */
interface ImageSaver {

    /** Saves [image] as a PNG named [name]; [onDone] says whether it was saved (false when refused or it failed). */
    fun save(image: ImageBitmap, name: String, onDone: (saved: Boolean) -> Unit)
}

/**
 * Remembers the platform [ImageSaver]: MediaStore's Pictures/Momentic on Android (asking for storage
 * only before Android 10), the Photos library on iOS (add-only access).
 */
@Composable
expect fun rememberImageSaver(): ImageSaver
