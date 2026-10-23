package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable

/**
 * `POST /auth/google`. Passing the device's [draftToken] signs in **and** moves everything the device
 * made onto the account in one call — the conversion moment; a second round trip risks the draft.
 */
@Serializable
data class GoogleSignInRequest(
    val idToken: String,
    val draftToken: String? = null,
)

/**
 * The sign-in response. The token never expires (HOA protocol 04): there is no refresh token to keep.
 * `API.md` still lists `expiresIn`/`refreshToken` from before the switch; they're ignored if present.
 */
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val user: UserDto? = null,
    /** Present only when a draft token was sent. */
    val claimed: ClaimedDto? = null,
)

@Serializable
data class UserDto(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null,
)

/** How much of the device's work moved onto the account. */
@Serializable
data class ClaimedDto(
    val creations: Int? = null,
    val images: Int? = null,
)

/** `POST /auth/claim-drafts` (needs a user). */
@Serializable
data class ClaimDraftsRequest(
    val draftToken: String,
)
