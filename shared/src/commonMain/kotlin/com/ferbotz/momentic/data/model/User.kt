package com.ferbotz.momentic.data.model

import kotlin.time.Instant

/** The signed-in account. */
data class User(
    val id: String,
    val email: String?,
    val name: String?,
    val avatarUrl: String?,
    val createdAt: Instant?,
)

/** Whether someone is signed in. Creating works either way; publishing needs [SignedIn]. */
sealed interface SessionState {

    data object SignedOut : SessionState

    /** [user] is the last profile seen — null only if a profile was never read on this install. */
    data class SignedIn(val user: User?) : SessionState
}

/**
 * The outcome of a sign-in. [claimedCreations] / [claimedImages] say how much of the device's
 * anonymous work moved onto the account in the same call.
 */
data class SignInResult(
    val user: User,
    val claimedCreations: Int,
    val claimedImages: Int,
)
