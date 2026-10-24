package com.ferbotz.momentic.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.moments.MomentStatus
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.card_featured
import com.ferbotz.momentic.resources.views_none
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import com.ferbotz.momentic.theme.occasionTint
import org.jetbrains.compose.resources.stringResource

/**
 * Template art or a photo from the server, over its occasion's tint — which is what shows while the
 * picture loads, and what stays when there's no picture yet (with the category's [emoji]).
 */
@Composable
fun ArtImage(
    url: String?,
    categoryId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    muted: Boolean = false,
) {
    Box(modifier.background(occasionTint(categoryId).brush), contentAlignment = Alignment.Center) {
        if (emoji != null) Text(emoji, fontSize = 44.sp)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                colorFilter = if (muted) MutedFilter else null,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Ended moments' covers lose a little colour. */
private val MutedFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.55f) })

/**
 * A template in a carousel (give it `Modifier.width(160.dp)`) or a grid cell (fill the cell): 4:5 art
 * with its tier badge, the name in serif, a two-line tagline and the price line exactly as the server
 * wrote it.
 */
@Composable
fun TemplateTile(
    name: String,
    tagline: String,
    priceLine: String,
    artUrl: String?,
    categoryId: String,
    tier: Tier?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
) {
    val c = Momentic.colors
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
                .clip(MomenticShapes.card),
        ) {
            ArtImage(artUrl, categoryId, contentDescription = null, modifier = Modifier.matchParentSize(), emoji = emoji)
            if (tier != null) TierBadge(tier, Modifier.padding(10.dp))
        }
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = Momentic.type.cardTitle, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(tagline, style = Momentic.type.caption, color = c.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(priceLine, style = Momentic.type.captionStrong, color = c.ink, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** [TemplateTile] for a feed card. */
@Composable
fun TemplateTile(card: TemplateCard, onClick: () -> Unit, modifier: Modifier = Modifier, emoji: String? = null) =
    TemplateTile(
        name = card.name,
        tagline = card.tagline,
        priceLine = card.displayPerDay,
        artUrl = card.thumbnailUrl,
        categoryId = card.categoryId,
        tier = Tier.fromKey(card.tierKey),
        onClick = onClick,
        modifier = modifier,
        emoji = emoji,
    )

/** The featured template at the top of Explore: wide art, "FEATURED", the name in display serif. */
@Composable
fun FeaturedTemplateCard(
    name: String,
    tagline: String,
    priceLine: String,
    artUrl: String?,
    categoryId: String,
    tier: Tier?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
) {
    val c = Momentic.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Raised, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            ArtImage(artUrl, categoryId, contentDescription = null, modifier = Modifier.matchParentSize(), emoji = emoji)
            if (tier != null) TierBadge(tier, Modifier.padding(12.dp))
        }
        Row(
            modifier = Modifier.padding(start = MomenticSpacing.gutter, end = MomenticSpacing.md, top = 14.dp, bottom = MomenticSpacing.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.card_featured).uppercase(), style = Momentic.type.eyebrow, color = c.rose)
                Text(name, style = Momentic.type.h1, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(tagline, style = Momentic.type.bodySmall, color = c.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(priceLine, style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = c.ink, modifier = Modifier.padding(top = 6.dp))
            }
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(c.petal),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MomenticIcons.Forward, contentDescription = null, tint = c.ink, modifier = Modifier.size(MomenticSizes.iconSmall))
            }
        }
    }
}

/**
 * One moment in My Moments: a 4:5 cover, the title in serif, a supporting line (the template, or when it
 * was edited), its status chip and — when it has been shared — its view count.
 *
 * @param views null hides the count (drafts); 0 reads "No views yet".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentRow(
    title: String,
    subtitle: String,
    coverUrl: String?,
    categoryId: String?,
    status: MomentStatus?,
    views: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtImage(
            url = coverUrl,
            categoryId = categoryId,
            contentDescription = null,
            muted = status == MomentStatus.Ended,
            modifier = Modifier
                .size(width = MomenticSizes.momentCover, height = 90.dp)
                .clip(MomenticShapes.input),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Momentic.type.cardTitle, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Momentic.type.caption, color = c.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (status != null) StatusChip(status)
                if (views != null) ViewCount(views)
            }
        }
        Icon(MomenticIcons.ChevronRight, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(MomenticSizes.iconSmall))
    }
}

@Composable
private fun ViewCount(views: Long) {
    val c = Momentic.colors
    if (views <= 0L) {
        Text(stringResource(Res.string.views_none), style = Momentic.type.caption, color = c.inkSoft)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(MomenticIcons.Eye, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(MomenticSizes.iconTiny))
            Text(views.toString(), style = Momentic.type.caption, color = c.inkSoft)
        }
    }
}
