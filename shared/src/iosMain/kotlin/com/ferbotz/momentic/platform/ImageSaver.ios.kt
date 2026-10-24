package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.ferbotz.momentic.data.media.toNSData
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { IosImageSaver() }

/** Add-only access to Photos (the prompt shows `NSPhotoLibraryAddUsageDescription`), then a new asset. */
private class IosImageSaver : ImageSaver {

    override fun save(image: ImageBitmap, name: String, onDone: (Boolean) -> Unit) {
        val png = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        val uiImage = png?.let { UIImage.imageWithData(it.toNSData()) } ?: return onDone(false)
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
            if (status != PHAuthorizationStatusAuthorized && status != PHAuthorizationStatusLimited) {
                dispatch_async(dispatch_get_main_queue()) { onDone(false) }
                return@requestAuthorizationForAccessLevel
            }
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = { PHAssetChangeRequest.creationRequestForAssetFromImage(uiImage) },
                completionHandler = { saved, _ -> dispatch_async(dispatch_get_main_queue()) { onDone(saved) } },
            )
        }
    }
}
