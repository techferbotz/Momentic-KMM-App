package com.ferbotz.momentic.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

/** Where a creation is in its life. The server owns it; the app never sends it. */
enum class CreationStatus {
    DRAFT,
    PUBLISHED,

    /** A status this build doesn't know yet. */
    UNKNOWN;

    companion object {
        fun fromWire(value: String?): CreationStatus = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/**
 * A creation — the user's filled-in template, a draft until it is published (`API.md` § Creations).
 *
 * `status`, `shareCode`, `expiresAt` and `viewCount` are server-owned: only [templateId], [data] and
 * [coverImageId] are ever written.
 *
 * @param data the personalization, keyed by the template's field keys. Images are `{ "imageId": … }`.
 * @param shareUrl the public link (`https://momentic.ferbotz.com/<shareCode>`) once published.
 */
data class Creation(
    val id: String,
    val templateId: String,
    val templateName: String,
    val status: CreationStatus,
    val data: JsonObject,
    val coverImageId: String?,
    val shareCode: String?,
    val shareUrl: String?,
    val publishedAt: Instant?,
    val expiresAt: Instant?,
    val isExpired: Boolean,
    val viewCount: Long,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    val isDraft: Boolean get() = status == CreationStatus.DRAFT
    val isPublished: Boolean get() = status == CreationStatus.PUBLISHED

    /** Published and still inside its paid window. */
    val isLive: Boolean get() = isPublished && !isExpired
}

/**
 * A short-lived link to the creator's own unpaid draft on the web (`POST /creations/:id/preview-token`).
 * It dies about 30 minutes after it is minted, so mint a fresh one each time rather than storing it.
 *
 * @param url the page, already resolved against the web origin.
 */
data class PreviewLink(
    val code: String,
    val url: String,
    val expiresAt: Instant?,
)

/** `GET /creations/:id/stats`. */
data class CreationStats(
    val totalViews: Long,
    val daysRemaining: Int?,
    val expiresAt: Instant?,
    val isExpired: Boolean,
    /** Views per day, oldest first — the backend sends the last 30 days. */
    val dailyViews: List<DailyViews>,
    /** Null when the template collects no RSVPs. */
    val rsvp: RsvpSummary?,
)

/** One day of [CreationStats.dailyViews]. [date] is null when the backend sends bare counts. */
data class DailyViews(
    val date: LocalDate?,
    val views: Long,
)

/** How guests have answered so far. */
data class RsvpSummary(
    val yes: Int,
    val no: Int,
    val maybe: Int,
    /** Total guests coming (each "yes" can bring more than one), when the backend reports it. */
    val guests: Int?,
) {
    val responses: Int get() = yes + no + maybe
}

/** A guest's answer. */
enum class Attendance {
    YES,
    NO,
    MAYBE,
    UNKNOWN;

    companion object {
        fun fromWire(value: String?): Attendance =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

/** One RSVP, as the creator reads it back (the public side can never read these). */
data class Rsvp(
    val id: String?,
    val guestName: String,
    val attending: Attendance,
    val guestCount: Int,
    val message: String?,
    val contactPhone: String?,
    val createdAt: Instant?,
)

/** One page of `GET /creations/:id/rsvps`, with the overall yes/no/maybe summary. */
data class RsvpPage(
    val page: Page<Rsvp>,
    val summary: RsvpSummary?,
)
