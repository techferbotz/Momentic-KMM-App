package com.ferbotz.momentic.data.mapper

import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.Category
import com.ferbotz.momentic.data.model.ChoiceOption
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.data.model.Pricing
import com.ferbotz.momentic.data.model.PricingBundle
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.network.dto.CategoryDto
import com.ferbotz.momentic.data.network.dto.FeedDto
import com.ferbotz.momentic.data.network.dto.FieldDefinitionDto
import com.ferbotz.momentic.data.network.dto.PricingDto
import com.ferbotz.momentic.data.network.dto.TemplateCardDto
import com.ferbotz.momentic.data.network.dto.TemplateDetailDto
import com.ferbotz.momentic.data.util.resolveWebUrl

/*
 * Catalogue wire → domain. Template art comes back as web paths and is resolved against
 * [webOrigin] here (contract BE-002), so nothing above the data layer ever joins a URL. Entries the
 * app can't use (no id, no name) are dropped rather than failing the whole feed.
 */

fun FeedDto.toDomain(webOrigin: String): List<FeedSection> =
    sections.orEmpty().mapNotNull { section ->
        val category = section.category?.toDomain() ?: return@mapNotNull null
        val templates = section.templates.orEmpty().mapNotNull { it.toDomain(webOrigin, category.id) }
        // An empty section has nothing to tap; the backend lists only live templates anyway.
        if (templates.isEmpty()) null else FeedSection(category, templates)
    }

fun CategoryDto.toDomain(): Category? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    return Category(id = id, name = name?.takeIf { it.isNotBlank() } ?: id, emoji = emoji.orEmpty())
}

fun TemplateCardDto.toDomain(webOrigin: String, fallbackCategoryId: String = ""): TemplateCard? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    return TemplateCard(
        id = id,
        categoryId = categoryId ?: fallbackCategoryId,
        name = name,
        tagline = tagline.orEmpty(),
        thumbnailUrl = resolveWebUrl(webOrigin, thumbnailUrl),
        tierKey = tierKey.orEmpty(),
        displayPerDay = displayPerDay.orEmpty(),
        tags = tags.orEmpty(),
        previewCode = previewCode ?: "pv_$id",
    )
}

fun TemplateDetailDto.toDomain(webOrigin: String): TemplateDetail? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val name = name?.takeIf { it.isNotBlank() } ?: return null
    val tier = tierKey ?: pricing?.tierKey.orEmpty()
    val perDay = displayPerDay ?: pricing?.displayPerDay.orEmpty()
    return TemplateDetail(
        id = id,
        categoryId = categoryId.orEmpty(),
        name = name,
        tagline = tagline.orEmpty(),
        description = description.orEmpty(),
        thumbnailUrl = resolveWebUrl(webOrigin, thumbnailUrl),
        previewImageUrls = previewImages.orEmpty().mapNotNull { resolveWebUrl(webOrigin, it) },
        tierKey = tier,
        displayPerDay = perDay,
        tags = tags.orEmpty(),
        previewCode = previewCode ?: "pv_$id",
        supportsRsvp = supportsRsvp ?: false,
        fields = fields.orEmpty().mapNotNull { it.toDomain(allowGroups = true) },
        pricing = pricing.toDomain(tier, perDay),
    )
}

/**
 * A field definition. Unknown types are kept (as [FieldType.UNKNOWN]) so the template can tell it
 * needs a newer build; nested groups are dropped, because a group never contains a group.
 */
fun FieldDefinitionDto.toDomain(allowGroups: Boolean): FieldDefinition? {
    val key = key?.takeIf { it.isNotBlank() } ?: return null
    val type = FieldType.fromWire(type)
    if (type == FieldType.GROUP_LIST && !allowGroups) return null
    return FieldDefinition(
        key = key,
        type = type,
        label = label?.takeIf { it.isNotBlank() } ?: key,
        required = required ?: false,
        helpText = helpText?.takeIf { it.isNotBlank() },
        placeholder = placeholder?.takeIf { it.isNotBlank() },
        defaultValue = defaultValue,
        maxLength = maxLength?.takeIf { it > 0 },
        min = min,
        max = max,
        minItems = minItems?.takeIf { it >= 0 },
        maxItems = maxItems?.takeIf { it > 0 },
        aspectRatio = AspectRatio.parse(aspectRatio),
        options = options.orEmpty().mapNotNull { option ->
            val value = option.value ?: return@mapNotNull null
            ChoiceOption(value = value, label = option.label?.takeIf { it.isNotBlank() } ?: value)
        },
        fields = if (type == FieldType.GROUP_LIST) fields.orEmpty().mapNotNull { it.toDomain(allowGroups = false) } else emptyList(),
    )
}

/** A creation's pricing (`GET /creations/:id/pricing`) — the same shape as a template's. */
fun PricingDto.toDomain(): Pricing = toDomain(tierKey.orEmpty(), displayPerDay.orEmpty())

private fun PricingDto?.toDomain(tierKey: String, displayPerDay: String): Pricing = Pricing(
    tierKey = this?.tierKey ?: tierKey,
    label = this?.label.orEmpty(),
    displayPerDay = this?.displayPerDay ?: displayPerDay,
    bundles = this?.bundles.orEmpty()
        .mapNotNull { bundle ->
            val days = bundle.days?.takeIf { it > 0 } ?: return@mapNotNull null
            val productId = bundle.productId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PricingBundle(days = days, productId = productId)
        }
        .sortedBy { it.days },
)
