package com.ferbotz.momentic.data.di

import com.ferbotz.momentic.data.platform.AppBuildInfo
import com.ferbotz.momentic.data.platform.iosAppBuildInfo
import com.ferbotz.momentic.presentation.di.presentationModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

/** iOS's platform module: the bundle's [AppBuildInfo]. */
fun iosDataModule(): Module = module {
    single<AppBuildInfo> { iosAppBuildInfo() }
}

/**
 * Entry point for starting Koin from Swift — call once, in the `App` initializer:
 *
 * ```swift
 * PlatformDataModule_iosKt.doInitKoinIos()
 * ```
 */
fun doInitKoinIos(): KoinApplication = initKoin(
    platformModule = iosDataModule(),
    additionalModules = listOf(presentationModule),
)
