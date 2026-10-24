package com.ferbotz.momentic.presentation.moments

import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Where a moment stands, as its status chip says it. */
sealed interface MomentStatus {
    /** Not published yet. */
    data object Draft : MomentStatus

    /** Live, with [daysLeft] calendar days to go (2 or more), or null when the end date is unknown. */
    data class Live(val daysLeft: Int?) : MomentStatus

    /** Live, but ending today ([daysLeft] 0) or tomorrow (1) — the warning chip. */
    data class EndingSoon(val daysLeft: Int) : MomentStatus

    /** Published, and its paid window is over. */
    data object Ended : MomentStatus
}

/**
 * The chip for this creation at [now]. Days are counted in calendar days in [timeZone], so a link that
 * ends tomorrow evening says "Ending tomorrow" all of today. Null for a status this build doesn't know.
 */
fun Creation.momentStatus(now: Instant, timeZone: TimeZone): MomentStatus? = when (status) {
    CreationStatus.DRAFT -> MomentStatus.Draft
    CreationStatus.UNKNOWN -> null
    CreationStatus.PUBLISHED -> {
        val end = expiresAt
        when {
            isExpired || (end != null && end <= now) -> MomentStatus.Ended
            end == null -> MomentStatus.Live(daysLeft = null)
            else -> {
                val days = now.toLocalDateTime(timeZone).date.daysUntil(end.toLocalDateTime(timeZone).date)
                if (days <= 1) MomentStatus.EndingSoon(days.coerceAtLeast(0)) else MomentStatus.Live(days)
            }
        }
    }
}
