package com.ferbotz.momentic.presentation.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.ferbotz.momentic.data.util.isWebUrl
import com.ferbotz.momentic.data.util.mailtoUri
import com.ferbotz.momentic.data.util.telUri
import com.ferbotz.momentic.platform.findActivity
import com.ferbotz.momentic.theme.Momentic

/** Custom Tabs, light, with the app's background as the toolbar. */
@Composable
actual fun rememberLinkOpener(): LinkOpener {
    val context = LocalContext.current
    val toolbarColor = Momentic.colors.background.toArgb()
    return remember(context, toolbarColor) { AndroidLinkOpener(context, toolbarColor) }
}

private class AndroidLinkOpener(
    private val context: Context,
    private val toolbarColor: Int,
) : LinkOpener {

    override fun openPage(url: String): Boolean {
        val target = url.trim()
        if (!isWebUrl(target)) return false
        val tab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_LIGHT)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build(),
            )
            .build()
        val activity = context.findActivity()
        // Outside an Activity (a preview, a wrapped context) the tab needs a task of its own.
        if (activity == null) tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            // A browser without Custom Tabs ignores the extras and opens the page itself, so the only
            // failure left is a device with no browser at all.
            tab.launchUrl(activity ?: context, Uri.parse(target))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override fun openOutside(url: String): Boolean {
        val target = url.trim()
        if (!isWebUrl(target)) return false
        // A plain VIEW: a Play Store link opens in the Play Store app, anything else in the browser.
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(target))
        val activity = context.findActivity()
        if (activity == null) view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            (activity ?: context).startActivity(view)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override fun call(phoneNumber: String): Boolean {
        val uri = telUri(phoneNumber) ?: return false
        // DIAL, not CALL: the number waits in the dialer, so no permission is needed and nothing rings by surprise.
        val dial = Intent(Intent.ACTION_DIAL, Uri.parse(uri))
        val activity = context.findActivity()
        if (activity == null) dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            (activity ?: context).startActivity(dial)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override fun composeEmail(address: String, subject: String?, body: String?, onUnavailable: () -> Unit) {
        // SENDTO + mailto: only mail apps answer it.
        val email = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUri(address, subject, body)))
        val activity = context.findActivity()
        if (activity == null) email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            (activity ?: context).startActivity(email)
        } catch (e: ActivityNotFoundException) {
            onUnavailable()
        }
    }
}
