package com.ferbotz.momentic.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The Activity behind a (possibly wrapped) context, or null if there isn't one. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
