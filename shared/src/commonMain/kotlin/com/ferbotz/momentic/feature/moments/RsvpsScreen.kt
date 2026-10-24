package com.ferbotz.momentic.feature.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.data.model.Attendance
import com.ferbotz.momentic.data.model.Rsvp
import com.ferbotz.momentic.data.model.RsvpSummary
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.IconButtonStyle
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.presentation.moments.RsvpsUiState
import com.ferbotz.momentic.presentation.util.initials
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.moment_guests
import com.ferbotz.momentic.resources.rsvps_answer_maybe
import com.ferbotz.momentic.resources.rsvps_answer_no
import com.ferbotz.momentic.resources.rsvps_answer_with_guests
import com.ferbotz.momentic.resources.rsvps_answer_yes
import com.ferbotz.momentic.resources.rsvps_call
import com.ferbotz.momentic.resources.rsvps_coming
import com.ferbotz.momentic.resources.rsvps_empty_body
import com.ferbotz.momentic.resources.rsvps_empty_title
import com.ferbotz.momentic.resources.rsvps_filter
import com.ferbotz.momentic.resources.rsvps_filter_empty
import com.ferbotz.momentic.resources.rsvps_list
import com.ferbotz.momentic.resources.rsvps_load_failed
import com.ferbotz.momentic.resources.rsvps_maybe
import com.ferbotz.momentic.resources.rsvps_no
import com.ferbotz.momentic.resources.rsvps_replies
import com.ferbotz.momentic.resources.rsvps_title
import com.ferbotz.momentic.resources.rsvps_yes
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import com.ferbotz.momentic.theme.occasionTint
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Guest replies: how many are coming, yes / maybe / no to filter by (on the phone — the list is the
 * server's), and each reply with the guest's number to call and their note.
 */
@Composable
fun RsvpsScreen(
    state: RsvpsUiState,
    momentTitle: String?,
    filter: Attendance?,
    onFilter: (Attendance?) -> Unit,
    onBack: () -> Unit,
    onCall: (phoneNumber: String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        MomenticTopBar(title = stringResource(Res.string.rsvps_title), subtitle = momentTitle, onBack = onBack, background = c.background)
        when {
            state.isLoading -> RsvpSkeleton()
            state.error != null && state.rsvps.isEmpty() -> EmptyState(
                illustration = MomenticIllustrations.BrokenLink,
                title = stringResource(Res.string.rsvps_load_failed),
                body = state.error,
                action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                modifier = Modifier.padding(MomenticSpacing.gutter).padding(top = MomenticSpacing.xl),
            )
            else -> ReplyList(state, filter, onFilter, onCall, onLoadMore)
        }
    }
}

@Composable
private fun ReplyList(state: RsvpsUiState, filter: Attendance?, onFilter: (Attendance?) -> Unit, onCall: (String) -> Unit, onLoadMore: () -> Unit) {
    val list = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 3 } }
    LaunchedEffect(nearEnd, state.canLoadMore) {
        if (nearEnd && state.canLoadMore) onLoadMore()
    }
    val shown = if (filter == null) state.rsvps else state.rsvps.filter { it.attending == filter }
    val listLabel = stringResource(Res.string.rsvps_list)
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().semantics { contentDescription = listLabel },
        contentPadding = PaddingValues(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs, bottom = MomenticSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        item(key = "summary") { SummaryCard(state.summary, state.rsvps, filter, onFilter) }
        when {
            state.rsvps.isEmpty() -> item(key = "empty") {
                EmptyState(
                    illustration = MomenticIllustrations.Replies,
                    title = stringResource(Res.string.rsvps_empty_title),
                    body = stringResource(Res.string.rsvps_empty_body),
                    modifier = Modifier.padding(top = MomenticSpacing.lg),
                )
            }
            shown.isEmpty() -> item(key = "filtered-empty") {
                Text(
                    stringResource(Res.string.rsvps_filter_empty),
                    style = Momentic.type.bodySmall,
                    color = Momentic.colors.inkSoft,
                    modifier = Modifier.fillMaxWidth().padding(MomenticSpacing.lg),
                )
            }
            else -> items(shown, key = { it.id ?: "${it.guestName}-${it.createdAt}" }) { ReplyCard(it, onCall) }
        }
        if (state.isLoadingMore) {
            item(key = "more") {
                Box(Modifier.fillMaxWidth().padding(MomenticSpacing.md), contentAlignment = Alignment.Center) { Spinner(color = Momentic.colors.rose, size = 22.dp) }
            }
        }
    }
}

/** How many are coming, the reply count, and the three answers as filters. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(summary: RsvpSummary?, replies: List<Rsvp>, filter: Attendance?, onFilter: (Attendance?) -> Unit) {
    val c = Momentic.colors
    val yes = summary?.yes ?: replies.count { it.attending == Attendance.YES }
    val maybe = summary?.maybe ?: replies.count { it.attending == Attendance.MAYBE }
    val no = summary?.no ?: replies.count { it.attending == Attendance.NO }
    val guests = summary?.guests ?: replies.filter { it.attending == Attendance.YES }.sumOf { it.guestCount }
    val responses = yes + maybe + no
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MomenticShapes.card)
            .background(occasionTint("wedding").brush)
            .padding(MomenticSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalAlignment = Alignment.Bottom) {
            Text(guests.toString(), style = Momentic.type.body.copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold), color = c.ink)
            Text(pluralStringResource(Res.plurals.rsvps_coming, guests), style = Momentic.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.padding(bottom = 6.dp))
            Text(pluralStringResource(Res.plurals.rsvps_replies, responses, responses), style = Momentic.type.caption, color = c.ink, modifier = Modifier.padding(bottom = 8.dp))
        }
        val filterLabel = stringResource(Res.string.rsvps_filter)
        FlowRow(
            modifier = Modifier.semantics { contentDescription = filterLabel },
            horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
        ) {
            AnswerFilter(stringResource(Res.string.rsvps_yes, yes), Attendance.YES, filter, onFilter)
            AnswerFilter(stringResource(Res.string.rsvps_maybe, maybe), Attendance.MAYBE, filter, onFilter)
            AnswerFilter(stringResource(Res.string.rsvps_no, no), Attendance.NO, filter, onFilter)
        }
    }
}

/** One answer as a filter pill; picked again, it lets go. Picked, it gets a ring. */
@Composable
private fun AnswerFilter(text: String, answer: Attendance, filter: Attendance?, onFilter: (Attendance?) -> Unit) {
    val (fill, ink, icon) = answer.palette()
    val selected = filter == answer
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(MomenticShapes.pill)
            .background(fill)
            .then(if (selected) Modifier.border(1.5.dp, ink, MomenticShapes.pill) else Modifier)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { on -> onFilter(if (on) answer else null) })
            .padding(start = 10.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (selected) MomenticIcons.Check else icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        Text(text, style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = ink)
    }
}

/** Each answer's tint, ink and mark — never colour alone. */
@Composable
private fun Attendance.palette(): Triple<Color, Color, ImageVector> {
    val c = Momentic.colors
    return when (this) {
        Attendance.YES -> Triple(c.successFill, c.success, MomenticIcons.CheckCircle)
        Attendance.MAYBE -> Triple(c.warningFill, c.warning, MomenticIcons.Help)
        Attendance.NO, Attendance.UNKNOWN -> Triple(c.endedFill, c.inkSoft, MomenticIcons.No)
    }
}

@Composable
private fun ReplyCard(rsvp: Rsvp, onCall: (String) -> Unit) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(c.surface)
            .padding(start = MomenticSpacing.md, end = MomenticSpacing.sm, top = 14.dp, bottom = MomenticSpacing.md),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.blush100), contentAlignment = Alignment.Center) {
                Text(initials(rsvp.guestName), style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = c.ink)
            }
            Column(Modifier.weight(1f)) {
                Text(rsvp.guestName, style = Momentic.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                rsvp.contactPhone?.let { Text(it, style = Momentic.type.bodySmall, color = c.inkSoft) }
            }
            rsvp.contactPhone?.let { phone ->
                MomenticIconButton(MomenticIcons.Phone, stringResource(Res.string.rsvps_call, rsvp.guestName), onClick = { onCall(phone) }, style = IconButtonStyle.Accent)
            }
        }
        AnswerChip(rsvp)
        rsvp.message?.takeIf { it.isNotBlank() }?.let { note ->
            Text("“${note.trim()}”", style = Momentic.type.quote.copy(fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 22.sp), color = c.ink)
        }
    }
}

/** "Yes · 2 guests", on the answer's tint. */
@Composable
private fun AnswerChip(rsvp: Rsvp) {
    val (fill, ink, icon) = rsvp.attending.palette()
    val answer = stringResource(
        when (rsvp.attending) {
            Attendance.YES -> Res.string.rsvps_answer_yes
            Attendance.MAYBE -> Res.string.rsvps_answer_maybe
            Attendance.NO, Attendance.UNKNOWN -> Res.string.rsvps_answer_no
        },
    )
    val text = if (rsvp.attending != Attendance.NO && rsvp.guestCount > 0) {
        stringResource(Res.string.rsvps_answer_with_guests, answer, pluralStringResource(Res.plurals.moment_guests, rsvp.guestCount, rsvp.guestCount))
    } else {
        answer
    }
    Row(
        Modifier.height(28.dp).clip(MomenticShapes.pill).background(fill).padding(start = 8.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(14.dp))
        Text(text, style = Momentic.type.captionStrong.copy(fontWeight = FontWeight.Bold), color = ink)
    }
}

@Composable
private fun RsvpSkeleton() {
    Column(Modifier.padding(MomenticSpacing.gutter), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        SkeletonBlock(Modifier.fillMaxWidth().height(116.dp), MomenticShapes.card)
        repeat(3) { SkeletonBlock(Modifier.fillMaxWidth().height(132.dp), MomenticShapes.card) }
    }
}
