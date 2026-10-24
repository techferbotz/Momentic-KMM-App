package com.ferbotz.momentic.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_back
import com.ferbotz.momentic.resources.save_not_saved
import com.ferbotz.momentic.resources.save_offline
import com.ferbotz.momentic.resources.save_saved
import com.ferbotz.momentic.resources.save_saving
import com.ferbotz.momentic.resources.tab_explore
import com.ferbotz.momentic.resources.tab_moments
import com.ferbotz.momentic.resources.tab_settings
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticMotion
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The three tabs. Creating starts from a template, so there's no "+" tab. */
enum class MainTab(internal val label: StringResource, internal val icon: ImageVector) {
    Explore(Res.string.tab_explore, MomenticIcons.Explore),
    Moments(Res.string.tab_moments, MomenticIcons.Moments),
    Settings(Res.string.tab_settings, MomenticIcons.Settings),
}

/**
 * The bottom bar on the three tab roots: cream, a hairline on top, the active tab's icon on a blush pill
 * with a rose label. Pads itself above the system's gesture area.
 */
@Composable
fun MomenticBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    Column(modifier.fillMaxWidth().background(c.surface)) {
        HorizontalDivider(thickness = 1.dp, color = c.line)
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(MomenticSizes.bottomBarHeight)
                .padding(horizontal = MomenticSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs),
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val pill by animateColorAsState(if (isSelected) c.blush100 else Color.Transparent, MomenticMotion.quick(), label = "tab")
                val content = if (isSelected) c.rose else c.inkSoft
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(MomenticSizes.bottomBarHeight)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs, Alignment.CenterVertically),
                ) {
                    Box(
                        modifier = Modifier.width(64.dp).height(32.dp).clip(MomenticShapes.pill).background(pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = content, modifier = Modifier.size(MomenticSizes.icon))
                    }
                    Text(
                        text = stringResource(tab.label),
                        style = Momentic.type.captionStrong.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold),
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * A pushed screen's top bar: back (or close), a serif title, actions on the right. Transparent by
 * default; [background] + [showDivider] give the cream, ruled bar of the in-app web view.
 */
@Composable
fun MomenticTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    navigationIcon: ImageVector = MomenticIcons.Back,
    navigationLabel: String = stringResource(Res.string.cd_back),
    background: Color = Color.Transparent,
    showDivider: Boolean = false,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Momentic.colors
    Column(modifier.fillMaxWidth().background(background).windowInsetsPadding(windowInsets)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MomenticSizes.topBarHeight).padding(horizontal = MomenticSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                MomenticIconButton(navigationIcon, navigationLabel, onBack)
                Spacer(Modifier.width(MomenticSpacing.xxs))
            } else {
                Spacer(Modifier.width(MomenticSpacing.sm))
            }
            Column(Modifier.weight(1f)) {
                if (title != null) {
                    Text(title, style = Momentic.type.h2, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (subtitle != null) {
                    Text(subtitle, style = Momentic.type.caption, color = c.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            actions()
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = c.line)
    }
}

/** A tab root's header: the serif display title, with an optional action (the "New moment" pill). */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Momentic.type.display, color = Momentic.colors.ink, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/**
 * The app's background behind the status bar, over a screen that scrolls under it — so the clock and
 * icons never sit on top of the content passing beneath.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier, color: Color = Momentic.colors.background) {
    Box(modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(color))
}

/** A small label pill in a top bar — "Example". */
@Composable
fun TopBarBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(MomenticShapes.pill)
            .background(Momentic.colors.blush100)
            .padding(horizontal = MomenticSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Momentic.type.captionStrong.copy(fontWeight = FontWeight.Bold), color = Momentic.colors.ink)
    }
}

/** Where the form's autosave is. */
enum class SaveStatus { Saving, Saved, Offline, NotSaved }

/** The quiet autosave note in the form's top bar. Announced politely when it changes. */
@Composable
fun SaveIndicator(status: SaveStatus, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val color = when (status) {
        SaveStatus.Saved -> c.success
        SaveStatus.NotSaved -> c.warning
        else -> c.inkSoft
    }
    Row(
        modifier = modifier
            .padding(horizontal = MomenticSpacing.sm)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            SaveStatus.Saving -> Spinner(color = c.inkSoft, size = 12.dp)
            SaveStatus.Saved -> Icon(MomenticIcons.CloudCheck, null, tint = color, modifier = Modifier.size(18.dp))
            SaveStatus.Offline -> Icon(MomenticIcons.WifiOff, null, tint = color, modifier = Modifier.size(16.dp))
            SaveStatus.NotSaved -> Icon(MomenticIcons.Alert, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(
            text = stringResource(
                when (status) {
                    SaveStatus.Saving -> Res.string.save_saving
                    SaveStatus.Saved -> Res.string.save_saved
                    SaveStatus.Offline -> Res.string.save_offline
                    SaveStatus.NotSaved -> Res.string.save_not_saved
                },
            ),
            style = Momentic.type.captionStrong,
            color = color,
            maxLines = 1,
        )
    }
}
