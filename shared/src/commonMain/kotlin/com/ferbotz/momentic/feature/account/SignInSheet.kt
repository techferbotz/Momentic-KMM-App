package com.ferbotz.momentic.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.platform.rememberGoogleSignIn
import com.ferbotz.momentic.presentation.account.SignInEvent
import com.ferbotz.momentic.presentation.account.SignInUiState
import com.ferbotz.momentic.presentation.account.SignInViewModel
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_close
import com.ferbotz.momentic.resources.signin_consent
import com.ferbotz.momentic.resources.signin_draft_comes
import com.ferbotz.momentic.resources.signin_google
import com.ferbotz.momentic.resources.signin_paused
import com.ferbotz.momentic.resources.signin_privacy
import com.ferbotz.momentic.resources.signin_terms
import com.ferbotz.momentic.resources.signin_title
import com.ferbotz.momentic.resources.signin_unavailable
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Sign in to publish — or from Settings. Signing in moves the device's drafts onto the account; then
 * [onSignedIn] runs (Publish goes on with the purchase). Google's own sheet does the signing in; this
 * build has none until the backend's web client id is in, and says so.
 */
@Composable
fun SignInSheet(onDismiss: () -> Unit, onSignedIn: () -> Unit) {
    val viewModel: SignInViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val links = rememberLinkOpener()
    val google = rememberGoogleSignIn()
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.SignedIn -> signedIn()
                is SignInEvent.OpenUrl -> links.openPage(event.url)
            }
        }
    }
    MomenticBottomSheet(onDismissRequest = onDismiss) {
        SignInContent(
            state = state,
            googleAvailable = google.isAvailable,
            onGoogle = {
                viewModel.onGoogleSignInStarted()
                google.signIn(
                    onIdToken = viewModel::onGoogleIdToken,
                    onCancelled = viewModel::onGoogleSignInCancelled,
                    onFailed = viewModel::onGoogleSignInFailed,
                )
            },
            onTerms = viewModel::openTerms,
            onPrivacy = viewModel::openPrivacyPolicy,
            onClose = dismiss,
        )
    }
}

/** The sheet's content: why to sign in, "Continue with Google" and the consent line with both legal links. */
@Composable
fun SignInContent(
    state: SignInUiState,
    googleAvailable: Boolean,
    onGoogle: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onClose: () -> Unit,
) {
    val c = Momentic.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.blush100), contentAlignment = Alignment.Center) {
            Icon(MomenticIcons.Link, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(26.dp))
        }
        Box(Modifier.weight(1f))
        MomenticIconButton(MomenticIcons.Close, stringResource(Res.string.cd_close), onClick = onClose)
    }
    Text(stringResource(Res.string.signin_title), style = Momentic.type.h1, color = c.ink, modifier = Modifier.padding(top = MomenticSpacing.md))
    Row(Modifier.padding(top = MomenticSpacing.sm), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Icon(MomenticIcons.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(MomenticSizes.iconSmall))
        Text(stringResource(Res.string.signin_draft_comes), style = Momentic.type.body, color = c.ink)
    }
    val note = when {
        !state.googleSignInEnabled -> stringResource(Res.string.signin_paused)
        !googleAvailable -> stringResource(Res.string.signin_unavailable)
        else -> null
    }
    note?.let { InlineBanner(it, MessageTone.Info, modifier = Modifier.padding(top = MomenticSpacing.md)) }
    state.error?.let { InlineBanner(it, MessageTone.Error, modifier = Modifier.padding(top = MomenticSpacing.md)) }
    GoogleButton(
        onClick = onGoogle,
        enabled = state.googleSignInEnabled && googleAvailable && !state.isSigningIn,
        loading = state.isSigningIn,
        modifier = Modifier.padding(top = MomenticSpacing.lg),
    )
    ConsentLine(onTerms, onPrivacy, Modifier.padding(top = MomenticSpacing.md))
}

/** Google's button as the canvas draws it: white, a hairline border, the G in a ring. */
@Composable
internal fun GoogleButton(onClick: () -> Unit, enabled: Boolean, loading: Boolean, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MomenticSizes.buttonHeight)
            .clip(MomenticShapes.pill)
            .background(Color.White)
            .border(1.5.dp, c.line, MomenticShapes.pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled || loading) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            Spinner(color = c.ink, size = 20.dp)
        } else {
            Box(Modifier.size(24.dp).border(1.5.dp, c.ink, CircleShape), contentAlignment = Alignment.Center) {
                Text("G", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            }
        }
        Text(stringResource(Res.string.signin_google), style = Momentic.type.button, color = c.ink)
    }
}

/**
 * "By continuing you agree to our Terms & Conditions and Privacy Policy" — both links from remote config.
 * On a blush card the links are ink ([onTint]); elsewhere rose.
 */
@Composable
internal fun ConsentLine(onTerms: () -> Unit, onPrivacy: () -> Unit, modifier: Modifier = Modifier, onTint: Boolean = false) {
    val c = Momentic.colors
    val terms = stringResource(Res.string.signin_terms)
    val privacy = stringResource(Res.string.signin_privacy)
    val template = stringResource(Res.string.signin_consent, TERMS, PRIVACY)
    val linkStyle = TextLinkStyles(SpanStyle(color = if (onTint) c.ink else c.rose, fontWeight = if (onTint) FontWeight.Bold else FontWeight.SemiBold))
    val text = buildAnnotatedString {
        var rest = template
        while (rest.isNotEmpty()) {
            val t = rest.indexOf(TERMS).takeIf { it >= 0 } ?: Int.MAX_VALUE
            val p = rest.indexOf(PRIVACY).takeIf { it >= 0 } ?: Int.MAX_VALUE
            val next = minOf(t, p)
            if (next == Int.MAX_VALUE) {
                append(rest)
                break
            }
            append(rest.substring(0, next))
            if (next == t) {
                withLink(LinkAnnotation.Clickable("terms", linkStyle) { onTerms() }) { append(terms) }
                rest = rest.substring(next + TERMS.length)
            } else {
                withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onPrivacy() }) { append(privacy) }
                rest = rest.substring(next + PRIVACY.length)
            }
        }
    }
    Text(
        text = text,
        style = Momentic.type.caption.copy(lineHeight = 18.sp),
        color = if (onTint) c.ink else c.inkSoft,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private const val TERMS = "\u0001terms\u0001"
private const val PRIVACY = "\u0001privacy\u0001"
