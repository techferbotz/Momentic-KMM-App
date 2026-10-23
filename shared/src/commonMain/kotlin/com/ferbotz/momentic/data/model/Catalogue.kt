package com.ferbotz.momentic.data.model

/** A feed category, e.g. 🎂 Birthday. */
data class Category(
    val id: String,
    val name: String,
    val emoji: String,
)

/**
 * A template as the feed shows it.
 *
 * @param thumbnailUrl absolute, already resolved against the web origin (contract BE-002); null when
 *   the backend sent none.
 * @param displayPerDay the backend's price copy ("from Rs.100/day") — shown as is, never computed.
 * @param previewCode `pv_<templateId>`: the shopfront's path on the web origin.
 */
data class TemplateCard(
    val id: String,
    val categoryId: String,
    val name: String,
    val tagline: String,
    val thumbnailUrl: String?,
    val tierKey: String,
    val displayPerDay: String,
    val tags: List<String>,
    val previewCode: String,
)

/** One category of the feed with its templates, in the backend's order. */
data class FeedSection(
    val category: Category,
    val templates: List<TemplateCard>,
)

/**
 * Everything the template screen and the input form need (`GET /templates/:id`).
 *
 * @param previewImageUrls absolute, resolved against the web origin like [thumbnailUrl].
 * @param fields the field contract the form is generated from — never hard-coded per template.
 */
data class TemplateDetail(
    val id: String,
    val categoryId: String,
    val name: String,
    val tagline: String,
    val description: String,
    val thumbnailUrl: String?,
    val previewImageUrls: List<String>,
    val tierKey: String,
    val displayPerDay: String,
    val tags: List<String>,
    val previewCode: String,
    val supportsRsvp: Boolean,
    val fields: List<FieldDefinition>,
    val pricing: Pricing,
) {
    /**
     * False when a required field is of a type this build can't render, so the user couldn't finish
     * the form. A new type ships with a backend deploy before any app knows it (templates need no app
     * release), so this is how an older build says "update to use this one" instead of failing later.
     */
    val canBeFilledIn: Boolean
        get() = fields.none { it.blocksFilling() }

    private fun FieldDefinition.blocksFilling(): Boolean =
        required && (type == FieldType.UNKNOWN || fields.any { it.blocksFilling() })
}

/** A template's price tier and the durations it can be bought for. */
data class Pricing(
    val tierKey: String,
    /** "Simple", "Signature", "Premium". */
    val label: String,
    val displayPerDay: String,
    /** Sorted by [PricingBundle.days]. */
    val bundles: List<PricingBundle>,
)

/** One purchasable duration: the link stays live for [days] after publishing. */
data class PricingBundle(
    val days: Int,
    /** The store product, e.g. `publish_tierB_7d`. */
    val productId: String,
)
