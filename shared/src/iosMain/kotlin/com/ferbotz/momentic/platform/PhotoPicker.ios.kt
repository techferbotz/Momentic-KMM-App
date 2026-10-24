package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.ferbotz.momentic.data.media.toByteArray
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): PhotoPicker {
    val deliver by rememberUpdatedState(onPicked)
    return remember { IosPhotoPicker { deliver(it) } }
}

/**
 * `PHPickerViewController`, images only, in the order picked. Each photo's data loads in the background;
 * the batch is handed over on the main queue once every one has answered.
 */
private class IosPhotoPicker(private val onPicked: (List<ByteArray>) -> Unit) : PhotoPicker {

    /** The picker holds its delegate weakly; this keeps it alive until the picker closes. */
    private var delegate: PickerDelegate? = null

    override fun pick(maxItems: Int) {
        val host = topViewController() ?: return
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = maxItems.coerceAtLeast(1).toLong()
            filter = PHPickerFilter.imagesFilter
            selection = PHPickerConfigurationSelectionOrdered
        }
        val picker = PHPickerViewController(configuration = configuration)
        val pickerDelegate = PickerDelegate { results ->
            delegate = null
            load(results)
        }
        delegate = pickerDelegate
        picker.delegate = pickerDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }

    private fun load(results: List<PHPickerResult>) {
        if (results.isEmpty()) return
        val loaded = arrayOfNulls<ByteArray>(results.size)
        var remaining = results.size
        results.forEachIndexed { index, result ->
            result.itemProvider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data: NSData?, _ ->
                val bytes = data?.toByteArray()
                dispatch_async(dispatch_get_main_queue()) {
                    loaded[index] = bytes
                    remaining -= 1
                    if (remaining == 0) {
                        val photos = loaded.filterNotNull()
                        if (photos.isNotEmpty()) onPicked(photos)
                    }
                }
            }
        }
    }
}

private class PickerDelegate(private val onFinish: (List<PHPickerResult>) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onFinish(didFinishPicking.filterIsInstance<PHPickerResult>())
    }
}
