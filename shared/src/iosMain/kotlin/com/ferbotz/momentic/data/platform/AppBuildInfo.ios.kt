package com.ferbotz.momentic.data.platform

import platform.Foundation.NSBundle

/**
 * iOS's [AppBuildInfo], read from the main bundle: `CFBundleShortVersionString` (marketing version)
 * and `CFBundleVersion` (the integer build). Missing or non-numeric values fall back to `"0"` / `0`,
 * which the update policy treats as "unknown build — never block".
 */
fun iosAppBuildInfo(): AppBuildInfo {
    val info = NSBundle.mainBundle.infoDictionary
    val versionName = info?.get("CFBundleShortVersionString") as? String ?: "0"
    val buildNumber = (info?.get("CFBundleVersion") as? String)?.trim()?.toLongOrNull() ?: 0L
    return AppBuildInfo(
        platform = AppPlatform.IOS,
        versionName = versionName,
        buildNumber = buildNumber,
    )
}
