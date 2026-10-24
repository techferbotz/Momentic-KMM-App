package com.ferbotz.momentic.presentation.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

/*
 * Dates as the app writes them — "Thu, 24 Sep", "30 October" — day before month, the way they're read in
 * India. English only for now, like the rest of the copy.
 */

/** "Thu, 24 Sep". */
fun LocalDate.shortLabel(): String = "${dayOfWeek.shortName()}, $day ${month.shortName()}"

/** "24 Sep". */
fun LocalDate.dayMonthLabel(): String = "$day ${month.shortName()}"

/** "30 October". */
fun LocalDate.longLabel(): String = "$day ${month.fullName()}"

/** "Sat, 12 December 2026" — a date field's value, the way guests will read it. */
fun LocalDate.fullLabel(): String = "${dayOfWeek.shortName()}, $day ${month.fullName()} $year"

/** "Sat, 12 Dec 2026" — the date half of a date-and-time field. */
fun LocalDate.compactLabel(): String = "${dayOfWeek.shortName()}, $day ${month.shortName()} $year"

/** "7:30 pm", "12:05 am" — a twelve-hour clock, as times are written on the pages. */
fun LocalTime.clockLabel(): String {
    val twelveHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$twelveHour:${minute.toString().padStart(2, '0')} ${if (hour < 12) "am" else "pm"}"
}

private fun DayOfWeek.shortName(): String = when (this) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

private fun Month.fullName(): String = when (this) {
    Month.JANUARY -> "January"
    Month.FEBRUARY -> "February"
    Month.MARCH -> "March"
    Month.APRIL -> "April"
    Month.MAY -> "May"
    Month.JUNE -> "June"
    Month.JULY -> "July"
    Month.AUGUST -> "August"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "December"
}

private fun Month.shortName(): String = fullName().take(3)
