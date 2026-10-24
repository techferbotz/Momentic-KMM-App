package com.ferbotz.momentic.presentation.publish

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * What [formattedPrice] for [days] days comes to a day, written the way the store wrote the price:
 * "₹599.00" for 7 days is "₹86". Whole units from 10 a day up; below that the store's own decimals
 * ("$4.99" for 7 days is "$0.71"). Null when the price has no number in it or [days] isn't positive.
 */
fun perDayPrice(formattedPrice: String, amountMicros: Long, days: Int): String? {
    if (days <= 0 || amountMicros < 0) return null
    val number = PRICE_NUMBER.find(formattedPrice) ?: return null
    val digits = number.value
    // The decimal separator is the last '.' or ',' when exactly two digits follow it.
    val separator = digits.lastOrNull { it == '.' || it == ',' }
        ?.takeIf { sep -> digits.substringAfterLast(sep).let { it.length == 2 && it.all(Char::isDigit) } }
    val perDay = amountMicros / 1_000_000.0 / days
    val written = if (perDay >= 10.0 || separator == null) {
        perDay.roundToLong().toString()
    } else {
        val cents = (perDay * 100).roundToLong()
        "${cents / 100}$separator${(cents % 100).toString().padStart(2, '0')}"
    }
    return formattedPrice.replaceRange(number.range, written)
}

private val PRICE_NUMBER = Regex("""\d[\d.,  ' ]*\d|\d""")

/**
 * The day a link bought now for [days] days stays live until: counted from [now] — or, renewing a link
 * that's still live, from when it would have ended ([currentExpiry]), since renewing early adds to the
 * time left.
 */
fun liveUntil(days: Int, now: Instant, currentExpiry: Instant?, timeZone: TimeZone): LocalDate {
    val from = currentExpiry?.takeIf { it > now } ?: now
    return (from + days.days).toLocalDateTime(timeZone).date
}
