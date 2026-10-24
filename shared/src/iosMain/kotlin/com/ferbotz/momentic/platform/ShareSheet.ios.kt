package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareSheet(): ShareSheet = remember { IosShareSheet() }

@Composable
actual fun rememberTextCopier(): TextCopier = remember {
    object : TextCopier {
        override fun copy(text: String) {
            UIPasteboard.generalPasteboard.string = text
        }
    }
}

private class IosShareSheet : ShareSheet {

    @OptIn(ExperimentalForeignApi::class)
    override fun share(text: String): Boolean {
        val host = topViewController() ?: return false
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        // On iPad the sheet is a popover and must be anchored, or UIKit raises; the centre of the screen will do.
        sheet.popoverPresentationController?.let { popover ->
            val view = host.view
            popover.sourceView = view
            view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
        }
        host.presentViewController(sheet, animated = true, completion = null)
        return true
    }

    override fun shareTo(target: ShareTarget, text: String): Boolean {
        val address = when (target) {
            ShareTarget.WhatsApp -> "whatsapp://send?text=${percentEncode(text)}"
            ShareTarget.Messages -> "sms:&body=${percentEncode(text)}"
            // Instagram has no way in for text; its share extension is in the sheet.
            ShareTarget.Instagram -> return share(text)
        }
        val url = NSURL.URLWithString(address) ?: return share(text)
        // Not installed: the answer comes back false, on the main queue — fall back to the sheet.
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) { opened ->
            if (!opened) share(text)
        }
        return true
    }
}
