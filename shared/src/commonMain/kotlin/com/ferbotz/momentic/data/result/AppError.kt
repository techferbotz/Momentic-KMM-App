package com.ferbotz.momentic.data.result

/**
 * A typed, presentation-agnostic failure. Repositories turn transport failures (HTTP statuses, the
 * envelope's `code`, dropped connections) into one of these, so the UI reacts without parsing HTTP.
 */
sealed class AppError {

    /** Human-readable reason, when the server or platform gave one. */
    abstract val message: String?

    /** No usable response — offline, DNS failure, timeout, TLS error. */
    data class Network(override val message: String? = null) : AppError()

    /**
     * The backend answered with a failure envelope. Branch on [code], never on [message]: the
     * message is user-facing copy and will be reworded (contract § Error codes).
     *
     * @param retryAfterSeconds set on `429 RATE_LIMITED`.
     */
    data class Api(
        val status: Int,
        val code: ErrorCode,
        override val message: String?,
        val retryAfterSeconds: Long? = null,
    ) : AppError()

    /**
     * Refused before anything was sent: the input breaks a limit the app already knows (a photo over
     * `limits.maxUploadBytes`, a type the server won't take). [message] is ready to show.
     */
    data class Invalid(override val message: String) : AppError()

    /** Something unexpected on the client side — an unreadable response, an illegal state. */
    data class Unknown(override val message: String? = null) : AppError()

    /** True when this is an API failure carrying [code]. */
    fun hasCode(code: ErrorCode): Boolean = this is Api && this.code == code
}

/**
 * The machine-readable failure codes from the contract (`API.md` § Error codes). A `404` reports a
 * row belonging to someone else as absent too, so [NOT_FOUND] means "you can't see it", not proof
 * that it doesn't exist.
 */
enum class ErrorCode {
    VALIDATION_FAILED,
    INVALID_TEMPLATE,
    AUTH_REQUIRED,
    INVALID_TOKEN,
    INVALID_GOOGLE_TOKEN,
    NOT_FOUND,
    DRAFT_LIMIT_REACHED,
    IMAGE_LIMIT_REACHED,
    RSVP_CLOSED,
    ALREADY_PUBLISHED,
    NOT_PUBLISHED,
    PURCHASE_NOT_VERIFIED,
    PURCHASE_ALREADY_USED,
    UNKNOWN_PRODUCT,
    RATE_LIMITED,
    STORAGE_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    INTERNAL,

    /** A code this build doesn't know yet, or none at all (a proxy's error page). */
    UNKNOWN;

    companion object {
        fun fromWire(code: String?): ErrorCode = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}
