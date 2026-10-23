package com.ferbotz.momentic.data.model

import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

/** What a render code points at; the code's shape decides which (`RENDER.md`). */
enum class RenderMode {
    /** `pv_<templateId>` — the template's sample data (the shopfront). */
    PREVIEW,

    /** `d_<jwt>` — the creator's own unpaid draft. */
    DRAFT,

    /** An 8-character share code inside its paid window. */
    PUBLISHED,

    /** The paid window has ended: [RenderedPage.values] is null, but the link-preview copy is intact. */
    EXPIRED,

    UNKNOWN;

    companion object {
        fun fromWire(value: String?): RenderMode = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

/** The link-preview copy, server-rendered into the page's `og:*` tags. */
data class OpenGraph(
    val title: String,
    val description: String,
    /** Absolute (resolved against the web origin). */
    val imageUrl: String?,
)

/**
 * One rendered page (`GET /public/render/{code}`). The app shows pages in the browser; it reads this
 * endpoint for a template's sample values — the same example the shopfront shows, so the app and the
 * web never disagree (contract BE-002).
 *
 * @param values keyed by the template's field keys, images already resolved to
 *   `{ id?, full, thumb, width, height }`; null when [mode] is [RenderMode.EXPIRED]. An optional
 *   field left empty is absent.
 */
data class RenderedPage(
    val templateId: String,
    val values: JsonObject?,
    val mode: RenderMode,
    val expiresAt: Instant?,
    val showBranding: Boolean,
    val og: OpenGraph,
    val rsvpEnabled: Boolean,
    val creationId: String?,
)
