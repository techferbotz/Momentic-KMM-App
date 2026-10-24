package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/**
 * The system photo picker — no permission needed on either platform: Android's photo picker, iOS's
 * `PHPickerViewController`. The picked photos come back as their original bytes, in the order picked.
 */
interface PhotoPicker {

    /** Opens the picker for up to [maxItems] photos (1 for a single photo). */
    fun pick(maxItems: Int)
}

/**
 * Remembers the platform [PhotoPicker]. [onPicked] gets the photos' bytes — nothing when the person
 * cancelled, and without any photo that couldn't be read.
 */
@Composable
expect fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): PhotoPicker
