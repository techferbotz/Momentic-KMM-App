package com.ferbotz.momentic.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareSheet(): ShareSheet {
    val context = LocalContext.current
    return remember(context) { AndroidShareSheet(context) }
}

private class AndroidShareSheet(private val context: Context) : ShareSheet {

    override fun share(text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, null)
        val activity = context.findActivity()
        if (activity == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            (activity ?: context).startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
