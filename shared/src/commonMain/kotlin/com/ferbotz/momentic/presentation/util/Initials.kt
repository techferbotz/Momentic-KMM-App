package com.ferbotz.momentic.presentation.util

/** "Meera Iyer" → "MI", "Arjun" → "A" — for an avatar with no picture. "?" when there's no name at all. */
fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}
