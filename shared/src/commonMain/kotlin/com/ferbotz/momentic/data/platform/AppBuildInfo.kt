package com.ferbotz.momentic.data.platform

/** Which platform build is calling — the wire value of the `X-App-Platform` header. */
enum class AppPlatform(val wire: String) {
    ANDROID("android"),
    IOS("ios"),
}

/**
 * Identifies this build to the backend. Sent on **every** request as `X-App-Platform`,
 * `X-App-Version` and `X-App-Build` (HOA protocol 11 §1) — remote config's targeting keys on them,
 * and a build that never sent them can't be targeted later. [buildNumber] is also what
 * `update.minSupportedBuild` / `recommendedBuild` are compared against.
 *
 * Supplied by the platform Koin module (Android reads `PackageInfo`, iOS the main bundle), so
 * shared code never hard-codes a version.
 *
 * @param versionName the display version, e.g. `1.4.2`.
 * @param buildNumber the integer build (Android `versionCode`, iOS `CFBundleVersion`); `0` when unknown.
 */
data class AppBuildInfo(
    val platform: AppPlatform,
    val versionName: String,
    val buildNumber: Long,
)
