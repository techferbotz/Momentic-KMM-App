package com.ferbotz.momentic.data.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Current wall-clock time in epoch milliseconds (cache timestamps, TTL checks). */
@OptIn(ExperimentalTime::class)
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
