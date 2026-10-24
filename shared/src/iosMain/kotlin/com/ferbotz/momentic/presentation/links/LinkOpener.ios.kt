package com.ferbotz.momentic.presentation.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ferbotz.momentic.data.util.isWebUrl
import com.ferbotz.momentic.data.util.mailtoUri
import com.ferbotz.momentic.platform.topViewController
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle

/** `SFSafariViewController` in light mode, presented over whatever Compose is currently showing. */
@Composable
actual fun rememberLinkOpener(): LinkOpener = remember { IosLinkOpener() }

private class IosLinkOpener : LinkOpener {

    override fun openPage(url: String): Boolean {
        val target = url.trim()
        // SFSafariViewController raises on any scheme but http(s), and an Objective-C exception can't
        // be caught from Kotlin — it would take the app down.
        if (!isWebUrl(target)) return false
        val nsUrl = NSURL.URLWithString(target) ?: return false
        val host = topViewController() ?: return false
        val safari = SFSafariViewController(uRL = nsUrl)
        safari.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
        host.presentViewController(safari, animated = true, completion = null)
        return true
    }

    override fun composeEmail(address: String, subject: String?, body: String?, onUnavailable: () -> Unit) {
        val url = NSURL.URLWithString(mailtoUri(address, subject, body)) ?: return onUnavailable()
        // The answer comes back on the main queue, which is where Compose state may be touched.
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) { opened ->
            if (!opened) onUnavailable()
        }
    }
}
