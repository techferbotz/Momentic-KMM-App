package com.ferbotz.momentic.presentation.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ferbotz.momentic.presentation.account.AccountViewModel
import com.ferbotz.momentic.presentation.account.SignInViewModel
import com.ferbotz.momentic.presentation.config.RemoteConfigViewModel
import com.ferbotz.momentic.presentation.editor.EditorViewModel
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import com.ferbotz.momentic.presentation.moments.MomentsViewModel
import com.ferbotz.momentic.presentation.moments.RsvpsViewModel
import com.ferbotz.momentic.presentation.publish.PublishViewModel
import com.ferbotz.momentic.presentation.session.AppSessionViewModel
import com.ferbotz.momentic.presentation.template.TemplateDetailViewModel
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * Koin wiring for the presentation layer: one view model per screen, each depending on repositories
 * from the data modules. Screen arguments (route ids) arrive as Koin parameters:
 *
 * ```
 * val vm: EditorViewModel = koinViewModel { parametersOf(templateId, creationId) }
 * ```
 */
val presentationModule: Module = module {
    // App root: remote config gate, and the session (sign-out notices, unfinished purchases).
    factory { RemoteConfigViewModel(get(), get()) }
    factory { AppSessionViewModel(get(), get()) }

    // Browse
    factory { FeedViewModel(get()) }
    factory { params -> TemplateDetailViewModel(templateId = params.get(), catalogue = get()) }

    // Create — parameters: templateId, then creationId (null for a new creation)
    factory { params ->
        EditorViewModel(
            templateId = params[0],
            creationId = params.values.getOrNull(1) as String?,
            catalogue = get(),
            creations = get(),
            images = get(),
            render = get(),
            processor = get(),
            remoteConfig = get(),
        )
    }

    // Publish — parameter: creationId
    factory { params ->
        PublishViewModel(
            creationId = params.get(),
            creations = get(),
            catalogue = get(),
            publishing = get(),
            billing = get(),
            auth = get(),
            remoteConfig = get(),
        )
    }

    // My moments
    factory { MomentsViewModel(get(), get(), get(), get(), get()) }
    factory { params -> MomentDetailViewModel(creationId = params.get(), creations = get(), catalogue = get(), remoteConfig = get(), events = get()) }
    factory { params -> RsvpsViewModel(creationId = params.get(), creations = get()) }

    // Account
    factory { SignInViewModel(get(), get()) }
    factory { AccountViewModel(get(), get(), get(), get()) }
}

/**
 * Obtains a [ViewModel] from Koin, scoped to the current `ViewModelStoreOwner` (the back-stack entry
 * under a `NavHost`, or the activity above it). Bridges Compose's `viewModel { }` factory to the
 * started global Koin without pulling in the koin-compose artifacts.
 *
 * @param parameters constructor arguments for a `factory { params -> … }` definition, e.g. a route id.
 */
@Composable
inline fun <reified T : ViewModel> koinViewModel(noinline parameters: ParametersDefinition? = null): T =
    viewModel { KoinPlatform.getKoin().get<T>(parameters = parameters) }

/**
 * The same, scoped to [owner] — a nav graph's back-stack entry, say, so the screens of one tab share a
 * view model (Explore and its categories share the one feed).
 */
@Composable
inline fun <reified T : ViewModel> koinViewModel(owner: ViewModelStoreOwner, noinline parameters: ParametersDefinition? = null): T =
    viewModel(viewModelStoreOwner = owner) { KoinPlatform.getKoin().get<T>(parameters = parameters) }

/**
 * Obtains a plain (non-[ViewModel]) singleton from Koin. Remembered, so the lookup happens once per
 * composition rather than on every recomposition.
 */
@Composable
inline fun <reified T : Any> koinGet(): T = remember { KoinPlatform.getKoin().get<T>() }
