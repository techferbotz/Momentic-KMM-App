package com.ferbotz.momentic.presentation.util

import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.ErrorCode

/**
 * Turns a typed [AppError] into a friendly message. One mapping, so every screen phrases the same
 * failure the same way. Branches on the machine-readable code; the backend's own message is used only
 * where it is the most specific thing available (a validation failure).
 */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> "You're offline. Check your connection and try again."
    is AppError.Invalid -> message
    is AppError.Unknown -> "Something went wrong. Please try again."
    is AppError.Api -> when (code) {
        ErrorCode.RATE_LIMITED -> retryAfterSeconds
            ?.let { "That's a lot at once. Please wait ${waitText(it)} and try again." }
            ?: "That's a lot at once. Please wait a moment and try again."
        ErrorCode.DRAFT_LIMIT_REACHED -> "You've reached the limit of saved drafts. Delete one to start another."
        ErrorCode.IMAGE_LIMIT_REACHED -> "You've reached the photo limit. Remove photos from another draft to add more."
        ErrorCode.STORAGE_UNAVAILABLE -> "Photo uploads are paused right now. Please try again a little later."
        ErrorCode.BILLING_UNAVAILABLE -> "Publishing isn't available just yet. Your draft is saved."
        ErrorCode.VALIDATION_FAILED -> message?.takeIf { it.isNotBlank() } ?: "Some details need another look."
        ErrorCode.NOT_FOUND -> "We couldn't find that. It may have been removed."
        ErrorCode.INVALID_TEMPLATE -> "This template isn't available any more."
        ErrorCode.AUTH_REQUIRED -> "Please sign in to continue."
        ErrorCode.INVALID_TOKEN -> "Please sign in again to continue."
        else -> if (status >= 500) {
            "Our servers hiccuped. Please try again in a moment."
        } else {
            message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
        }
    }
}

private fun waitText(seconds: Long): String = when {
    seconds <= 60 -> "a minute"
    else -> "${(seconds + 59) / 60} minutes"
}
