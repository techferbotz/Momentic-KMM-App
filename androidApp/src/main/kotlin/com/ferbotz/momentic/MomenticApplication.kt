package com.ferbotz.momentic

import android.app.Application
import com.ferbotz.momentic.data.di.androidDataModule
import com.ferbotz.momentic.data.di.initKoin
import com.ferbotz.momentic.presentation.di.presentationModule

/**
 * Starts Koin once at process start, handing the data layer the Android `Context` it needs (the
 * installed package's build info), plus the presentation view models. Registered in the manifest as
 * `android:name`.
 */
class MomenticApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            platformModule = androidDataModule(this),
            additionalModules = listOf(presentationModule),
        )
    }
}
