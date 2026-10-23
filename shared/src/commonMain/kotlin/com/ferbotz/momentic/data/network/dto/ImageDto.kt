package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable

/** `POST /images` → 201 `{ id, full, thumb, width, height }`. Also the resolved image shape in a render. */
@Serializable
data class UploadedImageDto(
    val id: String? = null,
    val full: String? = null,
    val thumb: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)
