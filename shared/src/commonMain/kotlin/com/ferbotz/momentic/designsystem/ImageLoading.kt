package com.ferbotz.momentic.designsystem

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ferbotz.momentic.theme.Momentic

/**
 * Sets the app-wide image loader: template art and uploaded photos come over Ktor and fade in (not with
 * reduced motion). Call once, inside the theme, at the root of every entry point — the app and the gallery.
 */
@Composable
fun ProvideMomenticImageLoader() {
    val fade = !Momentic.reduceMotion
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(fade)
            .build()
    }
}
