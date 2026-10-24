package com.ferbotz.momentic

import androidx.compose.ui.window.ComposeUIViewController
import com.ferbotz.momentic.designsystem.gallery.DesignGalleryApp

fun MainViewController() = ComposeUIViewController { App() }

/** The design gallery, for checking the design system on a device: run the scheme with `-designGallery`. */
fun DesignGalleryViewController() = ComposeUIViewController { DesignGalleryApp() }
