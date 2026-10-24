package com.ferbotz.momentic.presentation.editor

/**
 * What the form calls one entry of a repeating group, from the group's label: "Your moments" → moment,
 * so its buttons read "Add a moment", "Edit moment", "No moments yet". The view model's messages already
 * use the plural the same way ("Add at least 3 moments"). A label whose last word isn't a plural noun
 * ("Reasons I love you") falls back to item / items.
 */
data class EntryNoun(val singular: String, val plural: String) {

    /** "a moment", "an item". */
    val withArticle: String get() = (if (singular.firstOrNull()?.let { it.lowercaseChar() in VOWELS } == true) "an " else "a ") + singular

    companion object {
        private const val VOWELS = "aeiou"
        private val FALLBACK = EntryNoun("item", "items")

        fun forLabel(label: String): EntryNoun {
            val plural = label.trim().lowercase().removePrefix("your ").trim()
            if (plural.isEmpty()) return FALLBACK
            val lastWord = plural.substringAfterLast(' ')
            val singularWord = singularOf(lastWord) ?: return FALLBACK
            return EntryNoun(singular = plural.dropLast(lastWord.length) + singularWord, plural = plural)
        }

        /** The singular of an English plural, or null when [word] doesn't look like one. */
        private fun singularOf(word: String): String? = when {
            word.length > 3 && word.endsWith("ies") -> word.dropLast(3) + "y"
            listOf("sses", "shes", "ches", "xes", "zes").any(word::endsWith) -> word.dropLast(2)
            word.endsWith("ss") || word.endsWith("us") -> null
            word.length > 1 && word.endsWith("s") -> word.dropLast(1)
            else -> null
        }
    }
}
