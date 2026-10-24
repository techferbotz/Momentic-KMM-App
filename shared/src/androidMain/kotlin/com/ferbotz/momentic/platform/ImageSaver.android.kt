package com.ferbotz.momentic.platform

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val waiting = remember { arrayOfNulls<Triple<ImageBitmap, String, (Boolean) -> Unit>>(1) }
    // Before Android 10 writing to Pictures needs the storage permission; it's asked for only then.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val (image, name, onDone) = waiting[0] ?: return@rememberLauncherForActivityResult
        waiting[0] = null
        if (granted) scope.launch { onDone(write(context, image, name)) } else onDone(false)
    }
    return remember(context, permission) {
        object : ImageSaver {
            override fun save(image: ImageBitmap, name: String, onDone: (Boolean) -> Unit) {
                val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    waiting[0] = Triple(image, name, onDone)
                    permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    scope.launch { onDone(write(context, image, name)) }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private suspend fun write(context: Context, image: ImageBitmap, name: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = image.asAndroidBitmap()
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Momentic")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return@runCatching false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            MediaStore.Images.Media.insertImage(resolver, bitmap, name, null) != null
        }
    }.getOrDefault(false)
}
