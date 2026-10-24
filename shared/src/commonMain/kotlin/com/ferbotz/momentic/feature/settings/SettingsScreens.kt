package com.ferbotz.momentic.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.model.User
import com.ferbotz.momentic.designsystem.components.DestructiveButton
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.components.TabHeader
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.feature.account.ConsentLine
import com.ferbotz.momentic.feature.account.GoogleButton
import com.ferbotz.momentic.presentation.account.AccountUiState
import com.ferbotz.momentic.presentation.account.SignInUiState
import com.ferbotz.momentic.presentation.util.initials
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.delete_account_button
import com.ferbotz.momentic.resources.delete_account_erased
import com.ferbotz.momentic.resources.delete_account_heading
import com.ferbotz.momentic.resources.delete_account_more
import com.ferbotz.momentic.resources.delete_account_offline
import com.ferbotz.momentic.resources.delete_account_signed_in_as
import com.ferbotz.momentic.resources.delete_account_title
import com.ferbotz.momentic.resources.delete_account_what_happens
import com.ferbotz.momentic.resources.purchases_back
import com.ferbotz.momentic.resources.purchases_empty_body
import com.ferbotz.momentic.resources.purchases_empty_title
import com.ferbotz.momentic.resources.purchases_note
import com.ferbotz.momentic.resources.purchases_title
import com.ferbotz.momentic.resources.settings_about
import com.ferbotz.momentic.resources.settings_account
import com.ferbotz.momentic.resources.settings_actions
import com.ferbotz.momentic.resources.settings_delete_account
import com.ferbotz.momentic.resources.settings_opens_outside
import com.ferbotz.momentic.resources.settings_privacy
import com.ferbotz.momentic.resources.settings_purchases
import com.ferbotz.momentic.resources.settings_sign_out
import com.ferbotz.momentic.resources.settings_signin_title
import com.ferbotz.momentic.resources.settings_support
import com.ferbotz.momentic.resources.settings_terms
import com.ferbotz.momentic.resources.settings_version
import com.ferbotz.momentic.resources.signin_paused_plain
import com.ferbotz.momentic.resources.signin_unavailable_plain
import com.ferbotz.momentic.resources.tab_settings
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.stringResource

/**
 * Settings: the account (or, signed out, the invitation to sign in), the legal pages and support from
 * remote config, signing out and deleting the account, and the version.
 */
@Composable
fun SettingsScreen(
    state: AccountUiState,
    signIn: SignInUiState,
    googleAvailable: Boolean,
    onGoogle: () -> Unit,
    onPurchases: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onSupport: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    val session = state.session
    Column(modifier.fillMaxSize()) {
        TabHeader(stringResource(Res.string.tab_settings))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs, bottom = MomenticSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.md),
        ) {
            when (session) {
                is SessionState.SignedIn -> {
                    session.user?.let { AccountCard(it) }
                    SettingsGroup(stringResource(Res.string.settings_account)) {
                        SettingsRow(MomenticIcons.Receipt, stringResource(Res.string.settings_purchases), onPurchases, trailing = MomenticIcons.ChevronRight)
                    }
                }
                SessionState.SignedOut -> SignInCard(signIn, googleAvailable, onGoogle, onTerms, onPrivacy)
            }
            SettingsGroup(stringResource(Res.string.settings_about)) {
                SettingsRow(MomenticIcons.Shield, stringResource(Res.string.settings_privacy), onPrivacy, trailing = MomenticIcons.ExternalLink, outside = true)
                RowDivider()
                SettingsRow(MomenticIcons.Document, stringResource(Res.string.settings_terms), onTerms, trailing = MomenticIcons.ExternalLink, outside = true)
                RowDivider()
                SettingsRow(MomenticIcons.Mail, stringResource(Res.string.settings_support), onSupport, trailing = MomenticIcons.ExternalLink)
            }
            if (session is SessionState.SignedIn) {
                SettingsGroup(stringResource(Res.string.settings_actions)) {
                    SettingsRow(MomenticIcons.SignOut, stringResource(Res.string.settings_sign_out), onSignOut, busy = state.isSigningOut)
                    RowDivider()
                    SettingsRow(MomenticIcons.UserMinus, stringResource(Res.string.settings_delete_account), onDeleteAccount, trailing = MomenticIcons.ChevronRight, tint = c.error)
                }
            }
            if (state.appVersion.isNotBlank()) {
                Text(
                    stringResource(Res.string.settings_version, state.appVersion),
                    style = Momentic.type.caption,
                    color = c.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.xxs),
                )
            }
        }
    }
}

/** Who's signed in: their Google picture (or initials), name and email. */
@Composable
private fun AccountCard(user: User) {
    val c = Momentic.colors
    Row(
        Modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(c.surface)
            .padding(MomenticSpacing.md)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.blush100), contentAlignment = Alignment.Center) {
            // Sized to the circle, not the text setting: the avatar doesn't grow with it.
            Text(initials(user.name ?: user.email.orEmpty()), style = Momentic.type.h2.copy(fontSize = with(LocalDensity.current) { 20.dp.toSp() }), color = c.roseDeep)
            if (user.avatarUrl != null) {
                AsyncImage(model = user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            }
        }
        Column(Modifier.weight(1f)) {
            user.name?.let { Text(it, style = Momentic.type.h2, color = c.ink) }
            user.email?.let { Text(it, style = Momentic.type.bodySmall, color = c.inkSoft) }
        }
    }
}

/** Signed out: why to sign in, Google's button and the consent line, on blush. */
@Composable
private fun SignInCard(state: SignInUiState, googleAvailable: Boolean, onGoogle: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MomenticShapes.card)
            .background(Brush.linearGradient(listOf(c.blush100, c.blush200)))
            .padding(MomenticSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.md),
    ) {
        Text(stringResource(Res.string.settings_signin_title), style = Momentic.type.h2, color = c.ink, modifier = Modifier.semantics { heading() })
        val note = when {
            !state.googleSignInEnabled -> stringResource(Res.string.signin_paused_plain)
            !googleAvailable -> stringResource(Res.string.signin_unavailable_plain)
            else -> null
        }
        note?.let { InlineBanner(it, MessageTone.Neutral) }
        state.error?.let { InlineBanner(it, MessageTone.Error) }
        GoogleButton(onClick = onGoogle, enabled = state.googleSignInEnabled && googleAvailable && !state.isSigningIn, loading = state.isSigningIn)
        ConsentLine(onTerms, onPrivacy, onTint = true)
    }
}

/** A titled card of rows (the title is for screen readers; the cards speak for themselves on screen). */
@Composable
private fun SettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(Momentic.colors.surface)
            .semantics { contentDescription = label },
        content = content,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: ImageVector? = null,
    tint: Color = Momentic.colors.ink,
    outside: Boolean = false,
    busy: Boolean = false,
) {
    val c = Momentic.colors
    val hint = if (outside) stringResource(Res.string.settings_opens_outside) else null
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .then(if (hint != null) Modifier.semantics { contentDescription = "$label. $hint" } else Modifier)
            .padding(start = MomenticSpacing.md, end = MomenticSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = if (tint == c.ink) Momentic.type.body else Momentic.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
            modifier = Modifier.weight(1f),
        )
        if (busy) {
            Spinner(color = c.inkSoft, size = 18.dp)
        } else if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = if (tint == c.ink) c.inkSoft else tint, modifier = Modifier.size(if (trailing == MomenticIcons.ExternalLink) 18.dp else MomenticSizes.iconSmall))
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = Momentic.colors.line, modifier = Modifier.padding(start = 52.dp))
}

/**
 * Purchases. The backend's list of them (`GET /me/purchases`) isn't in the contract yet, so this says
 * what will show and how to ask about a payment meanwhile.
 */
@Composable
fun PurchasesScreen(onBack: () -> Unit, onSupport: () -> Unit, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        MomenticTopBar(title = stringResource(Res.string.purchases_title), onBack = onBack, navigationLabel = stringResource(Res.string.purchases_back), background = c.background)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MomenticSpacing.gutter),
        ) {
            Text(stringResource(Res.string.purchases_note), style = Momentic.type.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = MomenticSpacing.xs))
            EmptyState(
                illustration = MomenticIllustrations.Receipt,
                title = stringResource(Res.string.purchases_empty_title),
                body = stringResource(Res.string.purchases_empty_body),
                action = { QuietButton(stringResource(Res.string.settings_support), onClick = onSupport, icon = MomenticIcons.Mail) },
                modifier = Modifier.padding(top = MomenticSpacing.xxl),
            )
        }
    }
}

/**
 * What deleting the account does — links go offline at once, everything's erased after 30 days — the
 * page with the details, and "Delete my account" (confirmed by the route).
 */
@Composable
fun DeleteAccountScreen(
    email: String?,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        MomenticTopBar(title = stringResource(Res.string.delete_account_title), onBack = onBack, navigationLabel = stringResource(Res.string.purchases_back), background = c.background)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.md),
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(c.errorFill), contentAlignment = Alignment.Center) {
                Icon(MomenticIcons.UserMinus, contentDescription = null, tint = c.error, modifier = Modifier.size(26.dp))
            }
            Text(
                stringResource(Res.string.delete_account_heading),
                style = Momentic.type.h1,
                color = c.ink,
                modifier = Modifier.padding(top = MomenticSpacing.gutter).semantics { heading() },
            )
            email?.let {
                Text(stringResource(Res.string.delete_account_signed_in_as, it), style = Momentic.type.bodySmall, color = c.inkSoft, modifier = Modifier.padding(top = MomenticSpacing.xs))
            }
            Text(
                stringResource(Res.string.delete_account_what_happens).uppercase(),
                style = Momentic.type.eyebrow,
                color = c.inkSoft,
                modifier = Modifier.padding(top = 28.dp, bottom = MomenticSpacing.xs),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
                    .clip(MomenticShapes.card)
                    .background(c.surface),
            ) {
                ConsequenceRow(MomenticIcons.LinkOff, stringResource(Res.string.delete_account_offline))
                RowDivider()
                ConsequenceRow(MomenticIcons.Clock, stringResource(Res.string.delete_account_erased))
            }
            QuietButton(
                stringResource(Res.string.delete_account_more),
                onClick = onMore,
                icon = MomenticIcons.ExternalLink,
                compact = true,
                modifier = Modifier.padding(top = MomenticSpacing.xs),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.md)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            DestructiveButton(
                text = stringResource(Res.string.delete_account_button),
                onClick = onDelete,
                icon = MomenticIcons.Trash,
                modifier = Modifier.fillMaxWidth(),
                loading = isDeleting,
            )
        }
    }
}

@Composable
private fun ConsequenceRow(icon: ImageVector, text: String) {
    val c = Momentic.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = MomenticSpacing.md, vertical = MomenticSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.blush100), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(MomenticSizes.iconSmall))
        }
        Text(text, style = Momentic.type.body, color = c.ink, modifier = Modifier.weight(1f))
    }
}
