package com.ferbotz.momentic.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.platform.rememberGoogleSignIn
import com.ferbotz.momentic.presentation.account.AccountEvent
import com.ferbotz.momentic.presentation.account.AccountViewModel
import com.ferbotz.momentic.presentation.account.SignInEvent
import com.ferbotz.momentic.presentation.account.SignInViewModel
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.delete_account_cancel
import com.ferbotz.momentic.resources.delete_account_confirm
import com.ferbotz.momentic.resources.delete_account_confirm_body
import com.ferbotz.momentic.resources.delete_account_confirm_title
import com.ferbotz.momentic.resources.notice_account_deleted
import com.ferbotz.momentic.resources.notice_no_mail_app
import com.ferbotz.momentic.resources.notice_signed_out
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * The account's events, the same wherever the account view model is used: pages open in the in-app
 * browser, support opens the mail app (or says where to write), and signing out or deleting says so.
 */
@Composable
private fun AccountEvents(viewModel: AccountViewModel, onAccountDeleted: () -> Unit = {}) {
    val links = rememberLinkOpener()
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val deleted by rememberUpdatedState(onAccountDeleted)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountEvent.OpenUrl -> links.openPage(event.url)
                is AccountEvent.ComposeEmail -> links.composeEmail(event.address, event.subject, event.body) {
                    scope.launch { messenger.show(getString(Res.string.notice_no_mail_app, event.address)) }
                }
                AccountEvent.SignedOut -> messenger.show(getString(Res.string.notice_signed_out))
                AccountEvent.AccountDeleted -> {
                    deleted()
                    messenger.show(getString(Res.string.notice_account_deleted))
                }
            }
        }
    }
}

/**
 * The Settings tab. Legal pages and the support address come from remote config (HOA protocol 12),
 * never literals; signed out, it offers Google sign-in right here.
 */
@Composable
fun SettingsRoute(onPurchases: () -> Unit, onDeleteAccount: () -> Unit) {
    val viewModel: AccountViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    AccountEvents(viewModel)

    val signIn: SignInViewModel = koinViewModel()
    val signInState by signIn.uiState.collectAsStateWithLifecycle()
    val google = rememberGoogleSignIn()
    val links = rememberLinkOpener()
    LaunchedEffect(signIn) {
        signIn.events.collect { event ->
            when (event) {
                is SignInEvent.SignedIn -> viewModel.refreshProfile()
                is SignInEvent.OpenUrl -> links.openPage(event.url)
            }
        }
    }
    SettingsScreen(
        state = state,
        signIn = signInState,
        googleAvailable = google.isAvailable,
        onGoogle = {
            signIn.onGoogleSignInStarted()
            google.signIn(onIdToken = signIn::onGoogleIdToken, onCancelled = signIn::onGoogleSignInCancelled, onFailed = signIn::onGoogleSignInFailed)
        },
        onPurchases = onPurchases,
        onPrivacy = viewModel::openPrivacyPolicy,
        onTerms = viewModel::openTerms,
        onSupport = viewModel::contactSupport,
        onSignOut = viewModel::signOut,
        onDeleteAccount = onDeleteAccount,
    )
}

/** What the account has bought. The backend's purchases list isn't in the contract yet, so there's no view model. */
@Composable
fun PurchasesRoute(onBack: () -> Unit) {
    val viewModel: AccountViewModel = koinViewModel()
    AccountEvents(viewModel)
    PurchasesScreen(onBack = onBack, onSupport = viewModel::contactSupport)
}

/**
 * What deleting the account does, the web page that explains it, and the confirmation — the path Play
 * reviewers follow (Settings → Delete account). Once it's gone, [onDeleted] leaves this screen.
 */
@Composable
fun DeleteAccountRoute(onBack: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: AccountViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    AccountEvents(viewModel, onAccountDeleted = onDeleted)
    var confirming by rememberSaveable { mutableStateOf(false) }
    DeleteAccountScreen(
        email = (state.session as? SessionState.SignedIn)?.user?.email,
        onBack = onBack,
        onMore = viewModel::openDeleteAccountPage,
        onDelete = { confirming = true },
        isDeleting = state.isDeletingAccount,
    )
    if (confirming) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_account_confirm_title),
            message = stringResource(Res.string.delete_account_confirm_body),
            confirmLabel = stringResource(Res.string.delete_account_confirm),
            onConfirm = {
                confirming = false
                viewModel.deleteAccount()
            },
            dismissLabel = stringResource(Res.string.delete_account_cancel),
            onDismiss = { confirming = false },
            icon = MomenticIcons.Trash,
            confirming = state.isDeletingAccount,
        )
    }
}
