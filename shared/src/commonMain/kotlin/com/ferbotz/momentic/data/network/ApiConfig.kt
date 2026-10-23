package com.ferbotz.momentic.data.network

/**
 * Centralised network configuration: the two origins, the header names, and every endpoint path.
 * Keeping the paths here (rather than as string literals in the data sources) makes the surface
 * easy to audit against the contract's `API.md`.
 *
 * [BASE_URL] keeps a trailing slash and the paths below have no leading slash, so Ktor resolves
 * them as `BASE_URL + path`.
 */
object ApiConfig {

    /** The API (contract BE-003). Every endpoint lives under `/api/v1`. */
    const val BASE_URL: String = "https://momentic.ferbotz.com/api/v1/"

    /**
     * The web origin (contract BE-003): share links, template shopfronts, draft previews, and the
     * origin-relative `thumbnailUrl` / `previewImages` paths. It is **not** the API base — joining
     * those paths onto `…/api` gives a feed of broken thumbnails (BE-002). Remote config reports
     * the live value as `web.origin`; this is the compiled-in default.
     */
    const val WEB_ORIGIN: String = "https://momentic.ferbotz.com"

    /** The install id, sent on every request — remote config's experiment-bucketing key. */
    const val HEADER_DEVICE_ID: String = "X-Device-Id"

    /** The anonymous device principal (`POST /draft-sessions`). A user's bearer token wins over it. */
    const val HEADER_DRAFT_TOKEN: String = "X-Draft-Token"

    // Which build is calling (HOA protocol 11 §1). Optional for the backend, but a build that never
    // sent them can never be targeted by remote config, so every request carries all three.
    const val HEADER_APP_PLATFORM: String = "X-App-Platform"
    const val HEADER_APP_VERSION: String = "X-App-Version"
    const val HEADER_APP_BUILD: String = "X-App-Build"

    object Paths {
        /** Remote config (HOA protocol 10). No auth; fetched on every cold start. */
        const val CONFIG = "config"

        // Identity
        const val DRAFT_SESSIONS = "draft-sessions"
        const val AUTH_GOOGLE = "auth/google"
        const val AUTH_CLAIM_DRAFTS = "auth/claim-drafts"
        const val ME = "me"
        const val ME_PURCHASES = "me/purchases"

        // Catalogue — public and cacheable
        const val FEED = "feed"
        const val CATEGORIES = "categories"
        fun categoryTemplates(categoryId: String) = "categories/$categoryId/templates"
        fun template(templateId: String) = "templates/$templateId"

        // Creations (drafts) — device or user
        const val CREATIONS = "creations"
        fun creation(id: String) = "creations/$id"
        fun previewToken(creationId: String) = "creations/$creationId/preview-token"
        fun pricing(creationId: String) = "creations/$creationId/pricing"
        fun stats(creationId: String) = "creations/$creationId/stats"
        fun rsvps(creationId: String) = "creations/$creationId/rsvps"
        fun entitlements(creationId: String) = "creations/$creationId/entitlements"
        fun publish(creationId: String) = "creations/$creationId/publish"
        fun renew(creationId: String) = "creations/$creationId/renew"

        // Media
        const val IMAGES = "images"
        fun image(id: String) = "images/$id"

        /** The one public render endpoint; the code's shape (`pv_…`, `d_…`, 8 base62) decides what comes back. */
        fun render(code: String) = "public/render/$code"
    }
}
