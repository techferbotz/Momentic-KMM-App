package com.ferbotz.momentic.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.platform.rememberGoogleSignIn
import com.ferbotz.momentic.presentation.account.SignInEvent
import com.ferbotz.momentic.presentation.account.SignInViewModel
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing

/**
 * Sign in to publish — or from Settings. Signing in moves the device's drafts onto the account; then
 * [onSignedIn] runs (Publish goes on with the purchase). The platform Google sign-in arrives with the
 * backend's web client id.
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
        Text("Save your moment and get your link", style = Momentic.type.h1, color = Momentic.colors.ink)
        state.error?.let { InlineBanner(it, MessageTone.Error, modifier = Modifier.padding(top = MomenticSpacing.sm)) }
        if (state.googleSignInEnabled) {
            SecondaryButton(
                text = "Continue with Google",
                onClick = {
                    viewModel.onGoogleSignInStarted()
                    google.signIn(
                        onIdToken = viewModel::onGoogleIdToken,
                        onCancelled = viewModel::onGoogleSignInCancelled,
                        onFailed = viewModel::onGoogleSignInFailed,
                    )
                },
                loading = state.isSigningIn,
                modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.lg),
            )
        }
        QuietButton("Terms & Conditions", onClick = viewModel::openTerms, modifier = Modifier.fillMaxWidth())
        QuietButton("Privacy Policy", onClick = viewModel::openPrivacyPolicy, modifier = Modifier.fillMaxWidth())
    }
}
