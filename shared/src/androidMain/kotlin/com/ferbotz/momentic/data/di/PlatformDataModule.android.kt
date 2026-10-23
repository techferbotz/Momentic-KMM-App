package com.ferbotz.momentic.data.di

import android.content.Context
import com.ferbotz.momentic.data.platform.AppBuildInfo
import com.ferbotz.momentic.data.platform.androidAppBuildInfo
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android's platform module: the [AppBuildInfo] read from the installed package. Pass it to [initKoin]
 * from the `Application`:
 *
 * ```
 * initKoin(androidDataModule(this), listOf(presentationModule))
 * ```
 */
fun androidDataModule(context: Context): Module = module {
    single<AppBuildInfo> { androidAppBuildInfo(context) }
}
