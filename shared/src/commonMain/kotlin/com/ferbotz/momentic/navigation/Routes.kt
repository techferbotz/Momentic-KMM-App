package com.ferbotz.momentic.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe destinations for the app's NavHost. Only ids (and, for web pages, the address) ride the
 * routes — every screen loads what it shows, so a back-stack entry restored after process death still
 * works.
 *
 * The three tabs are nested graphs, so each keeps its own back stack: Explore (with Category, which also
 * shows the bottom bar), My Moments and Settings. Everything else is pushed over the tabs at the root and
 * hides the bottom bar. Sheets and dialogs (sign in, extend, delete) are screen state, not routes.
 */

/** The Explore tab: the feed and its categories. The start destination. */
@Serializable
data object ExploreGraph

/** The feed: categories of templates, a featured one first. */
@Serializable
data object ExploreRoute

/** One category of the feed, from the same feed the Explore tab loaded. */
@Serializable
data class CategoryRoute(val categoryId: String)

/** The My Moments tab. */
@Serializable
data object MomentsGraph

/** Drafts, live and ended moments. */
@Serializable
data object MomentsRoute

/** The Settings tab. */
@Serializable
data object SettingsGraph

/** The account, legal pages, support and sign out. */
@Serializable
data object SettingsRoute

/** One template: its art, what it asks for, its price, and "Make it yours". */
@Serializable
data class TemplateRoute(val templateId: String)

/**
 * A template's live example, inside the app's own frame. [title] heads the frame; with [templateId],
 * "Make it yours" under the page starts the form.
 */
@Serializable
data class WebPageRoute(val url: String, val title: String? = null, val templateId: String? = null)

/**
 * The input form generated from a template's field contract. [creationId] is null for a new
 * creation, or the draft to keep editing.
 */
@Serializable
data class EditorRoute(
    val templateId: String,
    val creationId: String? = null,
)

/** The creator's own draft on the web ([url] is a fresh preview link), with Edit and Publish under it. */
@Serializable
data class PreviewRoute(
    val url: String,
    val templateId: String,
    val creationId: String,
)

/** Choose how long, pay, publish — or renew a published moment for more days. */
@Serializable
data class PublishRoute(val creationId: String)

/** "Your moment is live": the link, sharing and the QR code. Pushed over My Moments. */
@Serializable
data class PublishedRoute(val creationId: String)

/** One moment: its link, views, time left, and what can be done with it. */
@Serializable
data class MomentRoute(val creationId: String)

/** The guests' replies to a moment that collects RSVPs. */
@Serializable
data class RsvpsRoute(val creationId: String)

/** What the account has bought. */
@Serializable
data object PurchasesRoute

/** What deleting the account removes, and the confirmation. */
@Serializable
data object DeleteAccountRoute
