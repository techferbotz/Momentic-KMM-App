package com.ferbotz.momentic.data.di

import com.ferbotz.momentic.data.billing.StoreBilling
import com.ferbotz.momentic.data.billing.UnavailableStoreBilling
import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.media.ImageProcessor
import com.ferbotz.momentic.data.media.platformImageProcessor
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.createHttpClient
import com.ferbotz.momentic.data.network.datasource.AuthRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.CatalogueRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.CreationRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.DraftSessionRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.ImageRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.PublishRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.RemoteConfigRemoteDataSource
import com.ferbotz.momentic.data.network.datasource.RenderRemoteDataSource
import com.ferbotz.momentic.data.repository.AuthRepository
import com.ferbotz.momentic.data.repository.AuthRepositoryImpl
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.repository.CatalogueRepositoryImpl
import com.ferbotz.momentic.data.repository.CreationEvents
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.repository.CreationRepositoryImpl
import com.ferbotz.momentic.data.repository.DraftSessionRepository
import com.ferbotz.momentic.data.repository.DraftSessionRepositoryImpl
import com.ferbotz.momentic.data.repository.ImageRepository
import com.ferbotz.momentic.data.repository.ImageRepositoryImpl
import com.ferbotz.momentic.data.repository.PrincipalGuard
import com.ferbotz.momentic.data.repository.PublishRepository
import com.ferbotz.momentic.data.repository.PublishRepositoryImpl
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.repository.RemoteConfigRepositoryImpl
import com.ferbotz.momentic.data.repository.RenderRepository
import com.ferbotz.momentic.data.repository.RenderRepositoryImpl
import com.ferbotz.momentic.data.session.SessionManager
import com.ferbotz.momentic.data.session.SignOutReason
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

/*
 * Koin wiring for the data layer, split by concern. The platform supplies one extra module (see
 * `platformDataModule`) with the `AppBuildInfo` — the only piece that needs platform context.
 * [dataModules] aggregates everything else; `initKoin` adds the platform module on top.
 */

/** JSON, preferences, the session, and the app-wide creation bus. */
val localModule: Module = module {
    single { appJson }
    single { Settings() }
    single { AppPreferences(get()) }
    single { CreationEvents() }
    // Owns the session for both teardown paths (user sign-out, and a token the server rejected). It
    // needs no HTTP, which is what lets the HTTP client depend on it below.
    single { SessionManager(get(), get()) }
    // Photos are made upright, cropped and shrunk on the device before upload.
    single<ImageProcessor> { platformImageProcessor() }
}

/** The shared HTTP client and the remote (API) data sources. */
val networkModule: Module = module {
    single {
        val sessionManager = get<SessionManager>()
        createHttpClient(
            prefs = get(),
            buildInfo = get(),
            // A 401 on a token-bearing request means the token was rejected, never that it timed out
            // (HOA protocol 04): discard it so the app stops retrying with a dead credential.
            onTokenRejected = { sessionManager.signOut(SignOutReason.TOKEN_REJECTED) },
        )
    }
    single { RemoteConfigRemoteDataSource(get()) }
    single { CatalogueRemoteDataSource(get()) }
    single { DraftSessionRemoteDataSource(get()) }
    single { CreationRemoteDataSource(get()) }
    single { ImageRemoteDataSource(get()) }
    single { RenderRemoteDataSource(get()) }
    single { AuthRemoteDataSource(get()) }
    single { PublishRemoteDataSource(get()) }
}

/** The repositories — bound to their interfaces so consumers depend on abstractions. */
val repositoryModule: Module = module {
    // A `single`, never a factory: it restores the cached document in its constructor and holds the
    // StateFlow every consumer observes. Two instances would fetch twice and disagree.
    single<RemoteConfigRepository> { RemoteConfigRepositoryImpl(get(), get()) }
    single<CatalogueRepository> {
        val remoteConfig = get<RemoteConfigRepository>()
        // Template art resolves against the live web origin, read when each response is mapped.
        CatalogueRepositoryImpl(get(), webOrigin = { remoteConfig.config.web.origin })
    }
    single<DraftSessionRepository> { DraftSessionRepositoryImpl(get(), get()) }
    single { PrincipalGuard(get(), get()) }
    single<CreationRepository> {
        val remoteConfig = get<RemoteConfigRepository>()
        CreationRepositoryImpl(get(), get(), get(), webOrigin = { remoteConfig.config.web.origin })
    }
    single<ImageRepository> {
        val remoteConfig = get<RemoteConfigRepository>()
        // Pre-flight checks use the limits the server reports it enforces.
        ImageRepositoryImpl(get(), get(), limits = { remoteConfig.config.limits })
    }
    single<RenderRepository> {
        val remoteConfig = get<RemoteConfigRepository>()
        RenderRepositoryImpl(get(), webOrigin = { remoteConfig.config.web.origin })
    }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<PublishRepository> { PublishRepositoryImpl(get(), get(), get()) }
    // No store keys in this build yet: purchases answer BILLING_UNAVAILABLE, as the backend does today.
    single<StoreBilling> { UnavailableStoreBilling() }
}

/** Every platform-agnostic data module. Combine with the platform module via `initKoin`. */
fun dataModules(): List<Module> = listOf(localModule, networkModule, repositoryModule)
