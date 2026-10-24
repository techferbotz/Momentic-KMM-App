package com.ferbotz.momentic.presentation.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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
