package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/** Where "Your moment is live" offers to send the link directly. */
enum class ShareTarget { WhatsApp, Instagram, Messages }

/** The system share sheet, and the apps a moment's link most often goes to. */
interface ShareSheet {

    /** Opens the share sheet with [text] (the message, ending in the link). False when nothing could take it. */
    fun share(text: String): Boolean

    /**
     * Opens [target] with [text] ready to send — or, where that app isn't installed or can't take text
     * from another app (Instagram on iOS), the share sheet.
     */
    fun shareTo(target: ShareTarget, text: String): Boolean
}

/** Remembers the platform [ShareSheet]: `ACTION_SEND` on Android, `UIActivityViewController` on iOS. */
@Composable
expect fun rememberShareSheet(): ShareSheet

/** Copies text — the link — to the clipboard. */
interface TextCopier {
    fun copy(text: String)
}

/** Remembers the platform clipboard: `ClipboardManager` on Android, `UIPasteboard` on iOS. */
@Composable
expect fun rememberTextCopier(): TextCopier

/** "Text, then the link" — how every share reads, whether or not [text] already ends with the link. */
fun shareText(text: String, url: String): String =
    if (text.contains(url)) text else listOf(text.trim(), url).filter { it.isNotEmpty() }.joinToString(" ")

/** [text] for a URL's query: UTF-8, with everything but letters, digits and `-._~` percent-encoded. */
fun percentEncode(text: String): String = buildString {
    for (byte in text.encodeToByteArray()) {
        val value = byte.toInt() and 0xFF
        val char = value.toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '-' || char == '.' || char == '_' || char == '~') {
            append(char)
        } else {
            append('%')
            append(HEX[value shr 4])
            append(HEX[value and 0x0F])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
