package com.ferbotz.momentic.feature.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.paywall_most_chosen
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/** One duration on offer: "7 days", what it comes to and until when, the store's price. */
data class DurationRow(
    val productId: String,
    val title: String,
    /** The line under the title when this row isn't chosen… */
    val subtitle: String,
    /** …and when it is (it adds "live until …" on the paywall). */
    val selectedSubtitle: String = subtitle,
    val price: String?,
    val mostChosen: Boolean = false,
)

/**
 * The durations as a radio group: the chosen one on blush with a rose ring. [compact] is the Extend
 * sheet's smaller rows. [label] names the group for screen readers ("How long it stays live").
 */
@Composable
fun DurationChoices(
    rows: List<DurationRow>,
    selectedId: String?,
    onSelect: (productId: String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth().selectableGroup().semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(if (compact) MomenticSpacing.xs else 10.dp),
    ) {
        rows.forEach { row -> DurationOptionRow(row, selected = row.productId == selectedId, compact = compact, onClick = { onSelect(row.productId) }) }
    }
}

@Composable
private fun DurationOptionRow(row: DurationRow, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val c = Momentic.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 56.dp else 64.dp)
            .clip(shape)
            .background(if (selected) c.blush100 else c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.rose else c.line, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = MomenticSpacing.md, vertical = MomenticSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(if (compact) MomenticSpacing.sm else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).border(2.dp, if (selected) c.rose else c.inkSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.rose))
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title,
                    style = Momentic.type.body.copy(fontWeight = FontWeight.Bold, fontSize = if (compact) 15.sp else 16.sp),
                    color = c.ink,
                )
                if (row.mostChosen) MostChosenBadge()
            }
            Text(
                text = if (selected) row.selectedSubtitle else row.subtitle,
                style = Momentic.type.caption,
                color = if (selected) c.ink else c.inkSoft,
            )
        }
        if (row.price != null) {
            Text(
                text = row.price,
                style = if (compact) Momentic.type.cardTitle.copy(fontSize = 18.sp) else Momentic.type.h2,
                color = c.ink,
            )
        }
    }
}

/** "Most chosen", white on rose-deep. */
@Composable
private fun MostChosenBadge() {
    val c = Momentic.colors
    Box(
        Modifier
            .height(22.dp)
            .clip(MomenticShapes.pill)
            .background(c.roseDeep)
            .padding(horizontal = MomenticSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(Res.string.paywall_most_chosen), style = Momentic.type.captionStrong.copy(fontWeight = FontWeight.Bold), color = c.white)
    }
}
