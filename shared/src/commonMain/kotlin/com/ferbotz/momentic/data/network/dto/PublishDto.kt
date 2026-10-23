package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `POST /creations/:id/publish` and `/renew` — the store transaction the backend verifies with RevenueCat. */
@Serializable
data class PublishRequest(
    val storeTransactionId: String,
)

/**
 * `{ creation, shareCode, shareUrl, expiresAt, alreadyPublished }`. `alreadyPublished: true` is the
 * idempotent answer to a repeated call with the same transaction — the same link, no second charge.
 */
@Serializable
data class PublishResponseDto(
    val creation: JsonObject? = null,
    val shareCode: String? = null,
    val shareUrl: String? = null,
    val expiresAt: String? = null,
    val alreadyPublished: Boolean? = null,
)
