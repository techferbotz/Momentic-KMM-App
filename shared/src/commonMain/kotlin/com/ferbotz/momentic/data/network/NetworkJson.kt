package com.ferbotz.momentic.data.network

import kotlinx.serialization.json.Json

/**
 * Shared kotlinx.serialization configuration used by the Ktor client and the preferences cache.
 *
 * - `ignoreUnknownKeys` — tolerate fields the backend adds later (the contract is additive).
 * - `explicitNulls = false` — omit null fields when encoding and treat absent keys as null.
 * - `isLenient` — be forgiving of minor format quirks.
 * - `coerceInputValues` — a `null` (or unknown enum) where a default exists falls back to it.
 */
val appJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
}
