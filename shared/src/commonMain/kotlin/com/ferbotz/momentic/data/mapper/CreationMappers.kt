package com.ferbotz.momentic.data.mapper

import com.ferbotz.momentic.data.model.Attendance
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.CreationStatus
import com.ferbotz.momentic.data.model.DailyViews
import com.ferbotz.momentic.data.model.Page
import com.ferbotz.momentic.data.model.Rsvp
import com.ferbotz.momentic.data.model.RsvpPage
import com.ferbotz.momentic.data.model.RsvpSummary
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.CreationDto
import com.ferbotz.momentic.data.network.dto.CreationsPageDto
import com.ferbotz.momentic.data.network.dto.StatsDto
import com.ferbotz.momentic.data.util.parseInstantOrNull
import com.ferbotz.momentic.data.util.parseLocalDateOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** A creation, or null when it has no id (nothing could be done with it). */
fun CreationDto.toDomain(): Creation? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    return Creation(
        id = id,
        templateId = templateId.orEmpty(),
        templateName = templateName.orEmpty(),
        status = CreationStatus.fromWire(status),
        data = data ?: JsonObject(emptyMap()),
        coverImageId = coverImageId,
        shareCode = shareCode,
        shareUrl = shareUrl,
        publishedAt = parseInstantOrNull(publishedAt),
        expiresAt = parseInstantOrNull(expiresAt),
        isExpired = isExpired ?: false,
        viewCount = viewCount ?: 0,
        createdAt = parseInstantOrNull(createdAt),
        updatedAt = parseInstantOrNull(updatedAt),
    )
}

fun CreationsPageDto.toDomain(): Page<Creation> =
    Page(items = creations.orEmpty().mapNotNull { it.toDomain() }, nextCursor = nextCursor)

/**
 * Reads a creation out of a response's `data`. `API.md` shows the creation itself for the CRUD
 * endpoints and `{ creation: … }` inside the publish response, so either shape is accepted.
 */
fun JsonObject.toCreationDto(): CreationDto =
    appJson.decodeFromJsonElement(this["creation"] as? JsonObject ?: this)

fun StatsDto.toDomain(): CreationStats = CreationStats(
    totalViews = totalViews ?: 0,
    daysRemaining = daysRemaining,
    expiresAt = parseInstantOrNull(expiresAt),
    isExpired = isExpired ?: false,
    dailyViews = series.orEmpty().mapNotNull { it.toDailyViews() },
    rsvp = rsvp.toRsvpSummary(),
)

/** A `series` entry: a bare count, or an object carrying a date and a count. */
private fun JsonElement.toDailyViews(): DailyViews? = when (this) {
    is JsonPrimitive -> longOrNull?.let { DailyViews(date = null, views = it) }
    is JsonObject -> {
        val views = firstLong("views", "count", "value", "total")
        views?.let { DailyViews(date = parseLocalDateOrNull(firstString("date", "day")), views = it) }
    }
    else -> null
}

/** The yes/no/maybe summary, whatever case its keys come in; null when absent. */
fun JsonElement?.toRsvpSummary(): RsvpSummary? {
    val obj = this as? JsonObject ?: return null
    val lower = obj.mapKeys { it.key.lowercase() }
    fun count(key: String): Int = (lower[key] as? JsonPrimitive)?.intOrNull ?: 0
    return RsvpSummary(
        yes = count("yes"),
        no = count("no"),
        maybe = count("maybe"),
        guests = listOf("guests", "guestcount", "totalguests", "attendingguests")
            .firstNotNullOfOrNull { (lower[it] as? JsonPrimitive)?.intOrNull },
    )
}

/**
 * `GET /creations/:id/rsvps` — items under a name of their own plus `nextCursor` (`API.md`
 * § Pagination) and a yes/no/maybe summary. The item and summary names aren't in the contract, so the
 * first array is taken as the items.
 */
fun JsonObject.toRsvpPage(): RsvpPage {
    val items = (this["rsvps"] as? JsonArray)
        ?: (this["items"] as? JsonArray)
        ?: values.firstOrNull { it is JsonArray } as? JsonArray
        ?: JsonArray(emptyList())
    val nextCursor = (this["nextCursor"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
    return RsvpPage(
        page = Page(items = items.mapNotNull { (it as? JsonObject)?.toRsvp() }, nextCursor = nextCursor),
        summary = this["summary"].toRsvpSummary(),
    )
}

private fun JsonObject.toRsvp(): Rsvp? {
    val name = firstString("guestName", "name") ?: return null
    return Rsvp(
        id = firstString("id"),
        guestName = name,
        attending = Attendance.fromWire(firstString("attending", "response")),
        guestCount = (this["guestCount"] as? JsonPrimitive)?.intOrNull ?: 1,
        message = firstString("message")?.takeIf { it.isNotBlank() },
        contactPhone = firstString("contactPhone", "phone")?.takeIf { it.isNotBlank() },
        createdAt = parseInstantOrNull(firstString("createdAt", "respondedAt")),
    )
}

private fun JsonObject.firstString(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull }

private fun JsonObject.firstLong(vararg keys: String): Long? =
    keys.firstNotNullOfOrNull { key ->
        val primitive = this[key] as? JsonPrimitive
        primitive?.longOrNull ?: primitive?.doubleOrNull?.toLong() ?: primitive?.booleanOrNull?.let { null }
    }
