package com.ferbotz.momentic.presentation.moments

import com.ferbotz.momentic.data.model.Creation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

/** The longest line of text that still reads as a moment's name. */
private const val TITLE_MAX = 40

/**
 * A name for a moment until creations carry a title of their own (a contract request): the first short
 * line of text it holds — usually who it's for ("Divya"); a date or a colour code doesn't count. Null
 * when it has none; show the template's name.
 */
fun Creation.interimTitle(): String? = data.values.firstNotNullOfOrNull { value ->
    (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()
        ?.takeIf { it.isNotEmpty() && it.length <= TITLE_MAX && '\n' !in it && !NOT_A_NAME.matches(it) }
}

/** A date or date-time ("2026-12-12", "2026-12-12T19:30:00+05:30") or a colour ("#B84D66"). */
private val NOT_A_NAME = Regex("""\d{4}-\d{2}-\d{2}.*|#[0-9A-Fa-f]{6}""")

/** How long ago a day was, the way the moment rows write it. */
sealed interface DayAgo {
    data object Today : DayAgo

    data object Yesterday : DayAgo

    /** Two to six days ago. */
    data class DaysAgo(val days: Int) : DayAgo

    /** A week or more ago: the date itself. */
    data class On(val date: LocalDate) : DayAgo
}

/** [at] against [now], in calendar days in [timeZone]. A time in the future (a clock that's ahead) is today. */
fun dayAgo(at: Instant, now: Instant, timeZone: TimeZone): DayAgo {
    val day = at.toLocalDateTime(timeZone).date
    return when (val days = day.daysUntil(now.toLocalDateTime(timeZone).date)) {
        in Int.MIN_VALUE..0 -> DayAgo.Today
        1 -> DayAgo.Yesterday
        in 2..6 -> DayAgo.DaysAgo(days)
        else -> DayAgo.On(day)
    }
}
