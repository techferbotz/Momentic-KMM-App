package com.ferbotz.momentic.data.util

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Parses an ISO-8601 timestamp from the API, or null when absent or unreadable (never throws). */
fun parseInstantOrNull(value: String?): Instant? =
    value?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it.trim()) }.getOrNull() }

/** Parses a `YYYY-MM-DD` date (or the date part of a timestamp), or null when unreadable. */
fun parseLocalDateOrNull(value: String?): LocalDate? =
    value?.takeIf { it.length >= 10 }?.let { runCatching { LocalDate.parse(it.trim().take(10)) }.getOrNull() }
