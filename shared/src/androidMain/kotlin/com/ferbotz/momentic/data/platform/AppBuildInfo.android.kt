package com.ferbotz.momentic.data.platform

import android.content.Context
import android.os.Build

/**
 * Android's [AppBuildInfo], read from the installed package: `versionName` and the (long)
 * `versionCode`. Falls back to `"0"` / `0` if the package info can't be read, which the update
 * policy treats as "unknown build — never block".
 */
fun androidAppBuildInfo(context: Context): AppBuildInfo {
    val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val buildNumber = when {
        info == null -> 0L
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode
        else -> @Suppress("DEPRECATION") info.versionCode.toLong()
    }
    return AppBuildInfo(
        platform = AppPlatform.ANDROID,
        versionName = info?.versionName ?: "0",
        buildNumber = buildNumber,
    )
}
