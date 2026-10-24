package com.ferbotz.momentic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ferbotz.momentic.designsystem.gallery.DesignGalleryApp

/**
 * Debug builds only: a second launcher icon, "Momentic gallery", that opens every design token and
 * component on one screen — for checking the design system on a real phone.
 */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DesignGalleryApp() }
    }
}
