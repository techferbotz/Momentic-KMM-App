package com.ferbotz.momentic.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe destinations for the app's NavHost. Only ids ride the routes — every screen loads what
 * it shows, so a back-stack entry restored after process death still works.
 */

/** The feed: categories of templates. The start destination. */
@Serializable
data object FeedRoute

/** One template: its art, what it asks for, its price, and "Use this template". */
@Serializable
data class TemplateRoute(val templateId: String)

/**
 * The input form generated from a template's field contract. [creationId] is null for a new
 * creation, or the draft to keep editing.
 */
@Serializable
data class EditorRoute(
    val templateId: String,
    val creationId: String? = null,
)
