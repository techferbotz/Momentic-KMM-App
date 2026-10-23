package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * The catalogue endpoints (`API.md` § Catalogue): public, cacheable, and what the feed is built from.
 * Every field is nullable so a backend addition — or an omission — never fails the decode; the
 * mapper drops entries it can't use and defaults the rest.
 */

/** `GET /feed`. */
@Serializable
data class FeedDto(
    val sections: List<FeedSectionDto>? = null,
)

@Serializable
data class FeedSectionDto(
    val category: CategoryDto? = null,
    val templates: List<TemplateCardDto>? = null,
)

/** `GET /categories`. */
@Serializable
data class CategoriesDto(
    val categories: List<CategoryDto>? = null,
)

@Serializable
data class CategoryDto(
    val id: String? = null,
    val name: String? = null,
    val emoji: String? = null,
)

/** A template card, as the feed and category listings return it. */
@Serializable
data class TemplateCardDto(
    val id: String? = null,
    val categoryId: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    /** A **web** path (`/templates/<id>/thumb.webp`) — resolve it against the web origin, not the API. */
    val thumbnailUrl: String? = null,
    val tierKey: String? = null,
    val displayPerDay: String? = null,
    val tags: List<String>? = null,
    /** `pv_<templateId>` — opens the shopfront, and fetches the sample values from the render endpoint. */
    val previewCode: String? = null,
)

/** `GET /templates/:id` — the card plus what the input screen needs. */
@Serializable
data class TemplateDetailDto(
    val id: String? = null,
    val categoryId: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    /** Web paths, like [thumbnailUrl]. */
    val previewImages: List<String>? = null,
    val tierKey: String? = null,
    val displayPerDay: String? = null,
    val tags: List<String>? = null,
    val previewCode: String? = null,
    val supportsRsvp: Boolean? = null,
    /** The field contract the input form is built from (`API.md` § The field contract). */
    val fields: List<FieldDefinitionDto>? = null,
    val pricing: PricingDto? = null,
)

/**
 * One entry of a template's `fields`. Every property beyond `key`, `type`, `label` and `required` is
 * optional and absent when it doesn't apply.
 */
@Serializable
data class FieldDefinitionDto(
    val key: String? = null,
    val type: String? = null,
    val label: String? = null,
    val required: Boolean? = null,
    val helpText: String? = null,
    val placeholder: String? = null,
    /** Any JSON value — its shape follows [type]. */
    val defaultValue: JsonElement? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    /** Cropper hint such as `"4:5"`. */
    val aspectRatio: String? = null,
    val options: List<ChoiceOptionDto>? = null,
    /** A `groupList`'s child fields — one level only. */
    val fields: List<FieldDefinitionDto>? = null,
)

@Serializable
data class ChoiceOptionDto(
    val value: String? = null,
    val label: String? = null,
)

@Serializable
data class PricingDto(
    val tierKey: String? = null,
    val label: String? = null,
    val displayPerDay: String? = null,
    val bundles: List<PricingBundleDto>? = null,
)

/** One purchasable duration. Prices are the store's; the app never computes money. */
@Serializable
data class PricingBundleDto(
    val days: Int? = null,
    val productId: String? = null,
)
