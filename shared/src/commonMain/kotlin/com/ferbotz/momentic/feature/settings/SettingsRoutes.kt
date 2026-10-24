package com.ferbotz.momentic.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.components.DestructiveButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.account.SignInSheet
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.presentation.account.AccountEvent
import com.ferbotz.momentic.presentation.account.AccountViewModel
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.notice_account_deleted
import com.ferbotz.momentic.resources.notice_no_mail_app
import com.ferbotz.momentic.resources.notice_signed_out
import com.ferbotz.momentic.resources.tab_settings
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
 * never literals; signed out, it offers sign in.
 */
@Composable
fun SettingsRoute(onPurchases: () -> Unit, onDeleteAccount: () -> Unit) {
    val viewModel: AccountViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    AccountEvents(viewModel)
    var signingIn by rememberSaveable { mutableStateOf(false) }
    StandInScreen(title = stringResource(Res.string.tab_settings), onBack = null, tabRoot = true) {
        when (val session = state.session) {
            is SessionState.SignedIn -> {
                StandInNote(listOfNotNull(session.user?.name, session.user?.email).joinToString(" · "))
                SecondaryButton("Purchases", onClick = onPurchases, icon = MomenticIcons.Receipt, modifier = Modifier.fillMaxWidth())
                SecondaryButton("Sign out", onClick = viewModel::signOut, icon = MomenticIcons.SignOut, loading = state.isSigningOut, modifier = Modifier.fillMaxWidth())
                DestructiveButton("Delete my account", onClick = onDeleteAccount, icon = MomenticIcons.UserMinus, modifier = Modifier.fillMaxWidth())
            }
            SessionState.SignedOut -> {
                StandInNote("Sign in to publish, and to see your moments on any phone.")
                PrimaryButton("Sign in", onClick = { signingIn = true }, modifier = Modifier.fillMaxWidth())
            }
        }
        QuietButton("Privacy Policy", onClick = viewModel::openPrivacyPolicy, modifier = Modifier.fillMaxWidth())
        QuietButton("Terms & Conditions", onClick = viewModel::openTerms, modifier = Modifier.fillMaxWidth())
        QuietButton("Contact support", onClick = viewModel::contactSupport, modifier = Modifier.fillMaxWidth())
        StandInNote("Version ${state.appVersion}")
    }
    if (signingIn) SignInSheet(onDismiss = { signingIn = false }, onSignedIn = { signingIn = false })
}

/** What the account has bought. The backend's purchases list isn't documented yet, so there's no view model. */
@Composable
fun PurchasesRoute(onBack: () -> Unit) {
    StandInScreen(title = "Purchases", onBack = onBack) {
        StandInNote("Your purchases will show here.")
    }
}

/**
 * What deleting the account removes, the web page that explains it, and the confirmation. Once it's
 * gone, [onDeleted] leaves this screen.
 */
@Composable
fun DeleteAccountRoute(onBack: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: AccountViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    AccountEvents(viewModel, onAccountDeleted = onDeleted)
    var confirming by rememberSaveable { mutableStateOf(false) }
    StandInScreen(title = "Delete account", onBack = onBack) {
        StandInNote("Your account, your moments and their links go for good.")
        QuietButton("What gets deleted", onClick = viewModel::openDeleteAccountPage, modifier = Modifier.fillMaxWidth())
        DestructiveButton("Delete my account", onClick = { confirming = true }, icon = MomenticIcons.Trash, modifier = Modifier.fillMaxWidth())
    }
    if (confirming) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "This can’t be undone.",
            confirmLabel = "Delete my account",
            onConfirm = {
                confirming = false
                viewModel.deleteAccount()
            },
            dismissLabel = "Keep my account",
            onDismiss = { confirming = false },
            icon = MomenticIcons.Trash,
            confirming = state.isDeletingAccount,
        )
    }
}
