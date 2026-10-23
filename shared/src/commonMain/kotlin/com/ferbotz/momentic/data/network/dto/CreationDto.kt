package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** The creation object (`API.md` § Creations). Every field optional on the wire; see the mapper. */
@Serializable
data class CreationDto(
    val id: String? = null,
    val templateId: String? = null,
    val templateName: String? = null,
    val status: String? = null,
    val data: JsonObject? = null,
    val coverImageId: String? = null,
    val shareCode: String? = null,
    val shareUrl: String? = null,
    val publishedAt: String? = null,
    val expiresAt: String? = null,
    val isExpired: Boolean? = null,
    val viewCount: Long? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** `GET /creations?cursor=&limit=` — newest first. */
@Serializable
data class CreationsPageDto(
    val creations: List<CreationDto>? = null,
    val nextCursor: String? = null,
)

/** `POST /creations`. `data` must satisfy the template's field contract. */
@Serializable
data class CreateCreationRequest(
    val templateId: String,
    val data: JsonObject,
    val coverImageId: String? = null,
)

/** `PUT /creations/:id` — only `templateId`, `data` and `coverImageId` are writable. */
@Serializable
data class UpdateCreationRequest(
    val data: JsonObject,
    val coverImageId: String? = null,
)

/** `POST /creations/:id/preview-token` → `{ code: "d_…", expiresAt }`. */
@Serializable
data class PreviewTokenDto(
    val code: String,
    val expiresAt: String? = null,
)

/**
 * `GET /creations/:id/stats` → `{ totalViews, daysRemaining, expiresAt, isExpired, series[30], rsvp }`.
 * `API.md` doesn't spell out a `series` entry or the `rsvp` object, so both are read leniently
 * (see `CreationMappers.kt`).
 */
@Serializable
data class StatsDto(
    val totalViews: Long? = null,
    val daysRemaining: Int? = null,
    val expiresAt: String? = null,
    val isExpired: Boolean? = null,
    val series: List<JsonElement>? = null,
    val rsvp: JsonElement? = null,
)
