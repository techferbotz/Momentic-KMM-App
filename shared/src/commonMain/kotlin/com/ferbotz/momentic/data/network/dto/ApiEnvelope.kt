package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable

/**
 * The response envelope every endpoint uses (contract § Domain conventions). Success is
 * `{ "success": true, "data": … }`; failure is `{ "success": false, "message": "…", "code": "…" }`.
 * There is no third shape.
 *
 * The data sources decode the body into this whatever the HTTP status, then unwrap it (see
 * `EnvelopeSupport.kt`).
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    /** User-facing copy — show it, never branch on it; the backend rewords it freely. */
    val message: String? = null,
    /** Machine-readable failure code: what the app branches on. */
    val code: String? = null,
)
