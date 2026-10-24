package com.ferbotz.momentic.feature.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.designsystem.components.ArtImage
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.feature.shell.UpdateNudge
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.maintenance_art
import com.ferbotz.momentic.resources.maintenance_auto
import com.ferbotz.momentic.resources.maintenance_title
import com.ferbotz.momentic.resources.update_art
import com.ferbotz.momentic.resources.update_banner
import com.ferbotz.momentic.resources.update_body
import com.ferbotz.momentic.resources.update_button
import com.ferbotz.momentic.resources.update_title
import com.ferbotz.momentic.resources.welcome_body
import com.ferbotz.momentic.resources.welcome_no_account
import com.ferbotz.momentic.resources.welcome_start
import com.ferbotz.momentic.resources.welcome_title
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import com.ferbotz.momentic.theme.occasionTint
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** How often the maintenance screen asks again whether Momentic is back. */
private const val MAINTENANCE_RETRY_MS = 30_000L

/**
 * Momentic is down for maintenance: the tending illustration, the server's message (or ours), and a
 * quiet note that the screen refreshes itself — it asks again every 30 seconds, so there's no button.
 */
@Composable
fun MaintenanceScreen(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val retry by rememberUpdatedState(onRetry)
    LaunchedEffect(Unit) {
        while (true) {
            delay(MAINTENANCE_RETRY_MS)
            retry()
        }
    }
    val c = Momentic.colors
    SystemScreen(modifier) {
        Image(
            MomenticIllustrations.Tending,
            contentDescription = stringResource(Res.string.maintenance_art),
            modifier = Modifier.size(width = 220.dp, height = 200.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            Text(
                text = message ?: stringResource(Res.string.maintenance_title),
                style = Momentic.type.h1,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.rose))
                Text(stringResource(Res.string.maintenance_auto), style = Momentic.type.bodySmall, color = c.inkSoft)
            }
        }
    }
}

/**
 * This build is too old to go on: the new-version illustration, why to update, and "Update" — shown
 * only once the store listing is known ([onUpdate] null hides it).
 */
@Composable
fun UpdateRequiredScreen(message: String?, onUpdate: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    SystemScreen(
        modifier,
        bottom = if (onUpdate == null) null else {
            { PrimaryButton(stringResource(Res.string.update_button), onClick = onUpdate, icon = MomenticIcons.Download, modifier = Modifier.fillMaxWidth()) }
        },
    ) {
        Image(
            MomenticIllustrations.NewVersion,
            contentDescription = stringResource(Res.string.update_art),
            modifier = Modifier.size(width = 220.dp, height = 200.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            Text(
                text = stringResource(Res.string.update_title),
                style = Momentic.type.h1,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = message ?: stringResource(Res.string.update_body),
                style = Momentic.type.body,
                color = c.inkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The recommended-update nudge as a banner, at the top of Explore. */
@Composable
fun UpdateBanner(nudge: UpdateNudge, modifier: Modifier = Modifier) {
    InlineBanner(
        text = nudge.message ?: stringResource(Res.string.update_banner),
        tone = MessageTone.Neutral,
        icon = MomenticIcons.Sparkle,
        actionLabel = nudge.onUpdate?.let { stringResource(Res.string.update_button) },
        onAction = nudge.onUpdate,
        onDismiss = nudge.onDismiss,
        modifier = modifier,
    )
}

/**
 * The first launch: three experiences fanned out on a blush arc, what Momentic does, and "Start
 * creating" — no account needed. [templates] are the feed's first few, for their art; until the feed
 * arrives (or without art) each card shows its occasion's tint.
 */
@Composable
fun WelcomeScreen(templates: List<TemplateCard>, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val shown = templates.take(3)
    val fallback = listOf("love", "birthday", "wedding")
    fun categoryAt(index: Int) = shown.getOrNull(index)?.categoryId ?: fallback[index]
    BoxWithConstraints(modifier.fillMaxSize().background(c.background)) {
        val arcHeight = minOf(maxHeight * 0.56f, 520.dp)
        // The blush arc behind the cards, 40 dp wider than the screen on each side so only its curve shows.
        Box(
            Modifier
                .offset(y = -(60.dp))
                .requiredSize(width = maxWidth + 80.dp, height = arcHeight + 60.dp)
                .clip(RoundedCornerShape(bottomStartPercent = 50, bottomEndPercent = 50))
                .background(occasionTint("greeting").brush),
        )
        // Pinned to the bottom when everything fits; scrolls (at large text sizes, on short screens) when not.
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val viewport = maxHeight
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewport)
                    .padding(horizontal = MomenticSpacing.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(arcHeight - 40.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(300.dp).alpha(0.4f).clip(CircleShape).background(c.surface))
                    FannedCard(shown.getOrNull(1), categoryAt(1), width = 150.dp, x = -(98.dp), y = 12.dp, raised = false)
                    FannedCard(shown.getOrNull(2), categoryAt(2), width = 150.dp, x = 98.dp, y = 4.dp, raised = false)
                    FannedCard(shown.getOrNull(0), categoryAt(0), width = 168.dp, x = 0.dp, y = -(12.dp), raised = true)
                }
                Spacer(Modifier.height(MomenticSpacing.lg))
                Text(
                    text = stringResource(Res.string.welcome_title),
                    style = Momentic.type.display,
                    color = c.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(MomenticSpacing.sm))
                Text(
                    text = stringResource(Res.string.welcome_body),
                    style = Momentic.type.body,
                    color = c.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 360.dp),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(MomenticSpacing.xl))
                PrimaryButton(stringResource(Res.string.welcome_start), onClick = onStart, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp))
                Row(
                    modifier = Modifier.padding(top = 14.dp, bottom = MomenticSpacing.md).semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(MomenticIcons.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.welcome_no_account), style = Momentic.type.bodySmall, color = c.inkSoft)
                }
            }
        }
    }
}

/** One of the three fanned-out experiences: 4:5 art, rounded, with a rose-tinted shadow. */
@Composable
private fun FannedCard(card: TemplateCard?, categoryId: String, width: Dp, x: Dp, y: Dp, raised: Boolean) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(width = width, height = width * 5 / 4)
            .momenticShadow(if (raised) MomenticElevation.Raised else MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .semantics { contentDescription = card?.name.orEmpty() },
    ) {
        ArtImage(card?.thumbnailUrl, categoryId, contentDescription = null, modifier = Modifier.matchParentSize(), emoji = EmojiFor[categoryId])
    }
}

private val EmojiFor = mapOf(
    "birthday" to "🎂",
    "wedding" to "💍",
    "love" to "💕",
    "anniversary" to "💐",
    "friendship" to "👫",
    "greeting" to "💌",
)

/** A full-screen system state: art and words centred, an optional action pinned at the bottom. */
@Composable
private fun SystemScreen(
    modifier: Modifier,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Momentic.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = MomenticSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Centred when it fits; scrolls when the text is large.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewport)
                    .padding(horizontal = MomenticSpacing.sm, vertical = MomenticSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            ) {
                content()
            }
        }
        if (bottom != null) {
            Box(Modifier.fillMaxWidth().widthIn(max = 480.dp).padding(bottom = MomenticSpacing.md)) { bottom() }
        }
    }
}
