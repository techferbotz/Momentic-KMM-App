package com.ferbotz.momentic.data.util

/**
 * Resolves a path the backend returned against the **web** origin (contract BE-002): template
 * `thumbnailUrl` / `previewImages`, and the sample images in a `pv_…` render, come back as
 * origin-relative paths like `/templates/greeting_open_letter/thumb.webp`. They are served by the
 * web renderer, not the API, so joining them onto the API base gives broken images.
 *
 * Absolute `http(s)` URLs pass through unchanged; a blank [path] resolves to null.
 */
fun resolveWebUrl(origin: String, path: String?): String? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (isWebUrl(value)) return value
    val base = origin.trim().trimEnd('/')
    return if (value.startsWith("/")) base + value else "$base/$value"
}

/**
 * True for an absolute `http`/`https` URL with a host — the only kind the in-app browsers take.
 * `SFSafariViewController` raises on any other scheme, and an Objective-C exception can't be caught
 * from Kotlin, so a bad link has to be stopped before it gets that far.
 */
fun isWebUrl(value: String): Boolean {
    if (value.any { it.isWhitespace() }) return false
    val rest = when {
        value.startsWith("https://", ignoreCase = true) -> value.substring("https://".length)
        value.startsWith("http://", ignoreCase = true) -> value.substring("http://".length)
        else -> return false
    }
    val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
    return authority.substringAfterLast('@').substringBefore(':').isNotEmpty()
}

/**
 * True for one plain email address — what can follow `mailto:` as it is. Anything that would turn it
 * into extra `mailto:` fields (`?`, `&`) or more recipients (`,`, `;`) is refused.
 */
fun isEmailAddress(value: String): Boolean {
    val at = value.indexOf('@')
    return at > 0 &&
        at == value.lastIndexOf('@') &&
        at < value.lastIndex &&
        value.none { it.isWhitespace() || it in "?&,;/<>\"" }
}

/**
 * A `mailto:` URI (RFC 6068) for [address] with an optional [subject] and [body], percent-encoded as
 * UTF-8. [address] is used as given, so it should already have passed [isEmailAddress].
 */
fun mailtoUri(address: String, subject: String? = null, body: String? = null): String {
    val fields = listOfNotNull(
        subject?.let { "subject=${percentEncode(it)}" },
        body?.let { "body=${percentEncode(it)}" },
    )
    val uri = "mailto:${address.trim()}"
    return if (fields.isEmpty()) uri else uri + "?" + fields.joinToString("&")
}

/** Percent-encodes all but RFC 3986's unreserved characters, byte by byte in UTF-8. */
private fun percentEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") {
            append(char)
        } else {
            append('%')
            append(HEX_DIGITS[code shr 4])
            append(HEX_DIGITS[code and 0x0F])
        }
    }
}

private const val HEX_DIGITS = "0123456789ABCDEF"
