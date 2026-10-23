package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable

/** `POST /draft-sessions` → the anonymous device principal, sent afterwards as `X-Draft-Token`. */
@Serializable
data class DraftSessionDto(
    val draftToken: String,
    val draftSessionId: String? = null,
)
