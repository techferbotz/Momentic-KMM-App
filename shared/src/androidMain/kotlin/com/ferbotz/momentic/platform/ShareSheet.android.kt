package com.ferbotz.momentic.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareSheet(): ShareSheet {
    val context = LocalContext.current
    return remember(context) { AndroidShareSheet(context) }
}

@Composable
actual fun rememberTextCopier(): TextCopier {
    val context = LocalContext.current
    return remember(context) {
        object : TextCopier {
            override fun copy(text: String) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
                clipboard.setPrimaryClip(ClipData.newPlainText("Momentic link", text))
            }
        }
    }
}

private class AndroidShareSheet(private val context: Context) : ShareSheet {

    override fun share(text: String): Boolean {
        val chooser = Intent.createChooser(sendText(text), null)
        return start(chooser)
    }

    override fun shareTo(target: ShareTarget, text: String): Boolean {
        val direct = when (target) {
            ShareTarget.WhatsApp -> sendText(text).setPackage("com.whatsapp")
            // Instagram takes shared text into a Direct message.
            ShareTarget.Instagram -> sendText(text).setPackage("com.instagram.android")
            ShareTarget.Messages -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", text)
        }
        return start(direct) || share(text)
    }

    private fun sendText(text: String) = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    private fun start(intent: Intent): Boolean {
        val activity = context.findActivity()
        if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            (activity ?: context).startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
