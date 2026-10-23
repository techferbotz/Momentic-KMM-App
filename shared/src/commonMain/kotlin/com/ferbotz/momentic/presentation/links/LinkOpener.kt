package com.ferbotz.momentic.presentation.links

import androidx.compose.runtime.Composable

/**
 * Opens the web pages the app hands off to: template shopfronts, draft previews, published moments,
 * and the legal pages (HOA protocol 12 §3) — and starts an email to support.
 *
 * Pages open in the platform's **in-app browser**: Custom Tabs on Android, `SFSafariViewController`
 * on iOS. A real browser rather than a WebView of our own: the templates are full web experiences
 * (animation, audio, the RSVP form), and the user gets an address bar and closes back onto the
 * screen they were on.
 *
 * Callers pass URLs built from remote config (`web.origin`, `links.*`), never literals, so a page can
 * move without a release.
 */
interface LinkOpener {

    /**
     * Shows [url] in the in-app browser (a plain browser where the device has no in-app one). Only
     * absolute `http`/`https` URLs are opened. Returns false when nothing was shown.
     */
    fun openPage(url: String): Boolean

    /**
     * Starts an email to [address] in the device's mail app, with [subject] and [body] filled in when
     * given. [onUnavailable] runs when no app takes it — possibly after this returns, because iOS
     * answers asynchronously.
     */
    fun composeEmail(
        address: String,
        subject: String? = null,
        body: String? = null,
        onUnavailable: () -> Unit = {},
    )
}

/** Remembers the platform [LinkOpener]; the platform bits (a `Context`, a view controller) come from the composition. */
@Composable
expect fun rememberLinkOpener(): LinkOpener
