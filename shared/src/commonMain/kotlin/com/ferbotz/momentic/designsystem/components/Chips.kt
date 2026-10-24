package com.ferbotz.momentic.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.moments.MomentStatus
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.status_draft
import com.ferbotz.momentic.resources.status_ended
import com.ferbotz.momentic.resources.status_ending_tomorrow
import com.ferbotz.momentic.resources.status_ends_today
import com.ferbotz.momentic.resources.status_live
import com.ferbotz.momentic.resources.status_live_days
import com.ferbotz.momentic.resources.tier_premium
import com.ferbotz.momentic.resources.tier_signature
import com.ferbotz.momentic.resources.tier_simple
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticMotion
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.momenticShadow
import com.ferbotz.momentic.theme.occasionTint
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * An occasion — "🎂 Birthday" — on its category's tint, 48 tall, ink label. Selected adds a rose-deep
 * ring and a check instead of the emoji.
 */
@Composable
fun OccasionChip(
    emoji: String,
    label: String,
    categoryId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val c = Momentic.colors
    val tint = occasionTint(categoryId)
    Row(
        modifier = modifier
            .height(MomenticSizes.chipHeight)
            .clip(MomenticShapes.pill)
            .background(tint.brush)
            .then(if (selected) Modifier.border(1.5.dp, c.roseDeep, MomenticShapes.pill) else Modifier)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(MomenticIcons.Check, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(18.dp))
        } else {
            Text(emoji, fontSize = 20.sp)
        }
        Text(label, style = Momentic.type.label, color = c.ink, maxLines = 1)
    }
}

/** A template's price tier. The feed only sends the key (`A`/`B`/`C`); the label is the app's. */
enum class Tier {
    Simple,
    Signature,
    Premium;

    companion object {
        fun fromKey(tierKey: String?): Tier? = when (tierKey?.trim()?.uppercase()) {
            "A" -> Simple
            "B" -> Signature
            "C" -> Premium
            else -> null
        }
    }
}

/**
 * The tier over template art: Simple on cream, Signature on blush with a sparkle, Premium on rose-deep
 * with a star (white text — rose-deep is the one fill white may sit on). [label] overrides the app's
 * name for the tier with the backend's own, when the screen has it.
 */
@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier, label: String? = null) {
    val c = Momentic.colors
    val text = label ?: stringResource(
        when (tier) {
            Tier.Simple -> Res.string.tier_simple
            Tier.Signature -> Res.string.tier_signature
            Tier.Premium -> Res.string.tier_premium
        },
    )
    val (background, content, icon) = when (tier) {
        Tier.Simple -> Triple(c.surface, c.ink, null)
        Tier.Signature -> Triple(c.blush200, c.ink, MomenticIcons.Sparkle)
        Tier.Premium -> Triple(c.roseDeep, c.white, MomenticIcons.Star)
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(MomenticShapes.pill)
            .background(background)
            .then(if (tier == Tier.Simple) Modifier.border(1.dp, c.line, MomenticShapes.pill) else Modifier)
            .padding(start = if (icon != null) 8.dp else 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (tier == Tier.Premium) c.white else c.roseDeep, modifier = Modifier.size(14.dp))
        }
        Text(text, style = Momentic.type.captionStrong, color = content, maxLines = 1)
    }
}

/** A moment's status in words and an icon, on its tint — never colour alone. */
@Composable
fun StatusChip(status: MomentStatus, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    when (status) {
        MomentStatus.Draft -> ChipShell(c.blush100, c.ink, modifier, icon = MomenticIcons.Pencil) {
            Text(stringResource(Res.string.status_draft), style = Momentic.type.captionStrong, color = c.ink)
        }
        is MomentStatus.Live -> ChipShell(c.successFill, c.success, modifier, dot = true) {
            val days = status.daysLeft
            Text(
                text = if (days == null) {
                    stringResource(Res.string.status_live)
                } else {
                    pluralStringResource(Res.plurals.status_live_days, days, days)
                },
                style = Momentic.type.captionStrong,
                color = c.success,
            )
        }
        is MomentStatus.EndingSoon -> ChipShell(c.warningFill, c.warning, modifier, icon = MomenticIcons.Clock) {
            Text(
                text = stringResource(if (status.daysLeft <= 0) Res.string.status_ends_today else Res.string.status_ending_tomorrow),
                style = Momentic.type.captionStrong,
                color = c.warning,
            )
        }
        MomentStatus.Ended -> ChipShell(c.endedFill, c.inkSoft, modifier, icon = MomenticIcons.EndedMark) {
            Text(stringResource(Res.string.status_ended), style = Momentic.type.captionStrong, color = c.inkSoft)
        }
    }
}

@Composable
private fun ChipShell(
    background: Color,
    content: Color,
    modifier: Modifier,
    icon: ImageVector? = null,
    dot: Boolean = false,
    text: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(MomenticShapes.pill)
            .background(background)
            .padding(start = if (icon != null) 8.dp else 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            dot -> Box(Modifier.size(8.dp).clip(CircleShape).background(content))
        }
        text()
    }
}

/** A template's tag — "Proposal", "Photo story". Not tappable. */
@Composable
fun TagLabel(text: String, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(MomenticShapes.pill)
            .background(c.blush100)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Momentic.type.captionStrong, color = c.ink, maxLines = 1)
    }
}

/**
 * Drafts / Live / Ended: equal segments, 48 tall, on a blush track; the chosen one is cream with a soft
 * shadow. Labels stay ink on the blush track (small text on blush is ink).
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MomenticShapes.pill)
            .background(c.blush100)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val background by animateColorAsState(if (selected) c.surface else Color.Transparent, MomenticMotion.quick(), label = "segment")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(MomenticSizes.minTouchTarget)
                    .then(if (selected) Modifier.momenticShadow(MomenticElevation.Rest, MomenticShapes.pill) else Modifier)
                    .clip(MomenticShapes.pill)
                    .background(background)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = if (selected) Momentic.type.label.copy(fontWeight = FontWeight.Bold) else Momentic.type.label,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
