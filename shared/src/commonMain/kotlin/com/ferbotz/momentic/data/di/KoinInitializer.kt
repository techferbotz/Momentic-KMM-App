package com.ferbotz.momentic.data.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts Koin with the platform module, all shared [dataModules], and any [additionalModules] the app
 * layer supplies (the presentation view models).
 *
 * Android passes `androidDataModule(context)` from `MomenticApplication`; iOS calls
 * `doInitKoinIos()`. Singletons are created lazily on first resolve, so calling this at start is cheap.
 */
fun initKoin(
    platformModule: Module,
    additionalModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(platformModule)
    modules(dataModules())
    modules(additionalModules)
}
