package com.ferbotz.momentic.data.mapper

import com.ferbotz.momentic.data.model.User
import com.ferbotz.momentic.data.network.dto.UserDto
import com.ferbotz.momentic.data.util.parseInstantOrNull

/** A user, or null when the payload carries no id. */
fun UserDto.toDomain(): User? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    return User(
        id = id,
        email = email?.takeIf { it.isNotBlank() },
        name = name?.takeIf { it.isNotBlank() },
        avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
        createdAt = parseInstantOrNull(createdAt),
    )
}

fun User.toDto(): UserDto = UserDto(
    id = id,
    email = email,
    name = name,
    avatarUrl = avatarUrl,
    createdAt = createdAt?.toString(),
)
