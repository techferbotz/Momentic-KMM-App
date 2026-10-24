package com.ferbotz.momentic.feature.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.OpenGraph
import com.ferbotz.momentic.designsystem.components.ArtImage
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.FallingPetals
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.platform.ShareTarget
import com.ferbotz.momentic.presentation.util.longLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.live_copy
import com.ferbotz.momentic.resources.live_copy_link
import com.ferbotz.momentic.resources.live_done
import com.ferbotz.momentic.resources.live_note
import com.ferbotz.momentic.resources.live_preview_caption
import com.ferbotz.momentic.resources.live_preview_description
import com.ferbotz.momentic.resources.live_qr
import com.ferbotz.momentic.resources.live_share
import com.ferbotz.momentic.resources.live_share_instagram
import com.ferbotz.momentic.resources.live_share_messages
import com.ferbotz.momentic.resources.live_share_more
import com.ferbotz.momentic.resources.live_share_whatsapp
import com.ferbotz.momentic.resources.live_title
import com.ferbotz.momentic.resources.live_until
import com.ferbotz.momentic.resources.paywall_load_failed
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.literal
import com.ferbotz.momentic.theme.momenticShadow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * "Your moment is live": until when, how the link will look in a chat, the link to copy, the apps it
 * usually goes to, and the QR code — with petals falling. [creation] is loaded fresh; [linkPreview] is
 * the page's own title and image, once they arrive.
 */
@Composable
fun PublishedScreen(
    creation: Creation?,
    linkPreview: OpenGraph?,
    categoryId: String?,
    isLoading: Boolean,
    error: String?,
    onDone: () -> Unit,
    onCopy: (url: String) -> Unit,
    onShareTo: (target: ShareTarget?) -> Unit,
    onShowQr: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    val url = creation?.shareUrl
    Box(modifier.fillMaxSize().background(c.background)) {
        Box(Modifier.fillMaxWidth().height(420.dp).background(Brush.verticalGradient(listOf(c.blush100, c.blush100.copy(alpha = 0f)))))
        FallingPetals(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.fillMaxWidth().height(MomenticSizes.topBarHeight).padding(horizontal = MomenticSpacing.xs), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                QuietButton(stringResource(Res.string.live_done), onClick = onDone, compact = true)
            }
            when {
                url != null -> LiveBody(creation, url, linkPreview, categoryId, onCopy, onShareTo, onShowQr)
                isLoading -> Box(Modifier.fillMaxWidth().padding(top = MomenticSpacing.huge), contentAlignment = Alignment.Center) {
                    Spinner(color = c.rose, size = 28.dp)
                }
                else -> EmptyState(
                    illustration = MomenticIllustrations.BrokenLink,
                    title = stringResource(Res.string.paywall_load_failed),
                    body = error.orEmpty(),
                    action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                    modifier = Modifier.padding(MomenticSpacing.gutter),
                )
            }
        }
    }
}

@Composable
private fun LiveBody(
    creation: Creation,
    url: String,
    linkPreview: OpenGraph?,
    categoryId: String?,
    onCopy: (String) -> Unit,
    onShareTo: (ShareTarget?) -> Unit,
    onShowQr: () -> Unit,
) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xxs, bottom = MomenticSpacing.lg)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.live_title),
            style = Momentic.type.display,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        creation.expiresAt?.let { end ->
            LivePill(stringResource(Res.string.live_until, end.toLocalDateTime(TimeZone.currentSystemDefault()).date.longLabel()), Modifier.padding(top = MomenticSpacing.sm))
        }
        Text(
            stringResource(Res.string.live_preview_caption),
            style = Momentic.type.caption,
            color = c.inkSoft,
            modifier = Modifier.padding(top = MomenticSpacing.lg, bottom = MomenticSpacing.xs),
        )
        LinkPreviewCard(linkPreview, fallbackTitle = creation.templateName, url = url, categoryId = categoryId)
        LinkRow(url, onCopy = { onCopy(url) }, modifier = Modifier.padding(top = MomenticSpacing.lg))
        ShareTargets(onShareTo, Modifier.padding(top = MomenticSpacing.gutter))
        QuietButton(stringResource(Res.string.live_qr), onClick = onShowQr, icon = MomenticIcons.Qr, modifier = Modifier.padding(top = MomenticSpacing.md))
        Text(
            stringResource(Res.string.live_note),
            style = Momentic.type.caption,
            color = c.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = MomenticSpacing.xs),
        )
    }
}

/** "Live until 30 October", green, with a haloed dot. */
@Composable
internal fun LivePill(text: String, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(MomenticShapes.pill)
            .background(c.successFill)
            .padding(horizontal = MomenticSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(c.success.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.success))
        }
        Text(text, style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = c.success)
    }
}

/** The page as a chat shows it: its image, its title and the site — a chat bubble's shape. */
@Composable
private fun LinkPreviewCard(preview: OpenGraph?, fallbackTitle: String, url: String, categoryId: String?) {
    val c = Momentic.colors
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
    val title = preview?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
    val host = url.removePrefix("https://").removePrefix("http://").substringBefore('/')
    val description = stringResource(Res.string.live_preview_description, title)
    Column(
        Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Raised, shape)
            .clip(shape)
            .background(c.surface)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        ArtImage(preview?.imageUrl, categoryId, contentDescription = null, modifier = Modifier.fillMaxWidth().height(156.dp))
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = MomenticSpacing.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(host, style = Momentic.type.caption.literal(), color = c.inkSoft)
        }
    }
}

/** The address in a pill, with "Copy". */
@Composable
private fun LinkRow(url: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val copyLabel = stringResource(Res.string.live_copy_link)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MomenticShapes.pill)
            .border(1.dp, c.line, MomenticShapes.pill)
            .background(c.surface)
            .padding(start = MomenticSpacing.md, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(MomenticIcons.Link, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(MomenticSizes.iconSmall))
        // The whole address on one line: it gives up a little size before it gives up characters.
        BasicText(
            text = url.removePrefix("https://").removePrefix("http://"),
            style = Momentic.type.label.copy(fontSize = 15.sp, color = c.ink).literal(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 15.sp),
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .height(44.dp)
                .clip(MomenticShapes.pill)
                .background(c.petal)
                .clickable(role = Role.Button, onClick = onCopy)
                .semantics { contentDescription = copyLabel }
                .padding(horizontal = MomenticSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(MomenticIcons.Copy, contentDescription = null, tint = c.ink, modifier = Modifier.size(MomenticSizes.iconTiny))
            Text(stringResource(Res.string.live_copy), style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = c.ink)
        }
    }
}

/** WhatsApp, Instagram, Messages — each opens that app with the message ready — and More, the share sheet. */
@Composable
private fun ShareTargets(onShareTo: (ShareTarget?) -> Unit, modifier: Modifier = Modifier) {
    val shareLabel = stringResource(Res.string.live_share)
    Row(modifier.fillMaxWidth().semantics { contentDescription = shareLabel }, horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
        ShareTargetButton(MomenticIcons.ChatRound, stringResource(Res.string.live_share_whatsapp), { onShareTo(ShareTarget.WhatsApp) }, Modifier.weight(1f))
        ShareTargetButton(MomenticIcons.Camera, stringResource(Res.string.live_share_instagram), { onShareTo(ShareTarget.Instagram) }, Modifier.weight(1f))
        ShareTargetButton(MomenticIcons.Message, stringResource(Res.string.live_share_messages), { onShareTo(ShareTarget.Messages) }, Modifier.weight(1f))
        ShareTargetButton(MomenticIcons.More, stringResource(Res.string.live_share_more), { onShareTo(null) }, Modifier.weight(1f))
    }
}

@Composable
private fun ShareTargetButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Column(
        modifier = modifier.clip(MomenticShapes.input).clickable(role = Role.Button, onClick = onClick).padding(vertical = MomenticSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).border(1.dp, c.line, CircleShape).background(c.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(MomenticSizes.icon))
        }
        Text(label, style = Momentic.type.captionStrong, color = c.ink, maxLines = 1)
    }
}
