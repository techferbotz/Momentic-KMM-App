package com.ferbotz.momentic.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Android photo picker (`PickVisualMedia`; on older devices the backport or the document picker).
 * The multi-select contract needs a limit above one, so a single photo uses the single-select one.
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (List<ByteArray>) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliver by rememberUpdatedState(onPicked)
    val multipleContract = remember { PickPhotos() }

    fun read(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { uris.mapNotNull { context.readBytes(it) } }
            if (bytes.isNotEmpty()) deliver(bytes)
        }
    }

    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> read(listOfNotNull(uri)) }
    val multiple = rememberLauncherForActivityResult(multipleContract) { uris -> read(uris) }

    return remember(single, multiple) {
        object : PhotoPicker {
            override fun pick(maxItems: Int) {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (maxItems <= 1) {
                    single.launch(request)
                } else {
                    multipleContract.limit = maxItems
                    multiple.launch(request)
                }
            }
        }
    }
}

/** Multi-select, with the limit read when the picker opens rather than fixed when the screen was built. */
private class PickPhotos : ActivityResultContract<PickVisualMediaRequest, List<Uri>>() {
    var limit: Int = 2

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent =
        ActivityResultContracts.PickMultipleVisualMedia(limit.coerceAtLeast(2)).createIntent(context, input)

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> =
        ActivityResultContracts.PickMultipleVisualMedia().parseResult(resultCode, intent)
}

private fun Context.readBytes(uri: Uri): ByteArray? =
    runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
