package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `GET /public/render/{code}` (the contract's `RENDER.md`). */
@Serializable
data class RenderDto(
    val templateId: String? = null,
    /** The personalization with images resolved; `null` when the link has expired. */
    val values: JsonObject? = null,
    val meta: RenderMetaDto? = null,
    val og: OpenGraphDto? = null,
    val features: RenderFeaturesDto? = null,
    /** Published links only — the key for the view ping. */
    val creationId: String? = null,
)

@Serializable
data class RenderMetaDto(
    val mode: String? = null,
    val expiresAt: String? = null,
    val showBranding: Boolean? = null,
)

@Serializable
data class OpenGraphDto(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class RenderFeaturesDto(
    val rsvp: Boolean? = null,
)
