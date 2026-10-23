package com.ferbotz.momentic.data.model

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.platform.AppPlatform

/**
 * The remote config document (`GET /api/v1/config`) as the app uses it — every field non-null.
 *
 * The **constructor defaults are the compiled-in defaults** and equal the backend's own defaults
 * (= today's behaviour, per the backend's `docs/REMOTE_CONFIG.md` § 2): honouring the config changes
 * nothing until the backend deliberately flips a value. A key missing from the wire resolves to the
 * value here, leaf by leaf — see `RemoteConfigMappers.kt`.
 */
data class RemoteConfig(
    val features: Features = Features(),
    val update: UpdatePolicy = UpdatePolicy(),
    val maintenance: Maintenance = Maintenance(),
    val limits: Limits = Limits(),
    val web: Web = Web(),
    val links: Links = Links(),
) {

    /**
     * Kill switches for UI **entry points**. Turning one off hides the surface in the app; the API
     * itself keeps working (closing an API is a server decision).
     */
    data class Features(
        /** "Continue with Google". */
        val googleLogin: Boolean = true,
        /** The paywall and the publish purchase. */
        val publishing: Boolean = true,
        /** "Extend this link" on a published moment. */
        val renewal: Boolean = true,
        /** Views and RSVPs on a published moment. */
        val stats: Boolean = true,
    )

    /**
     * Update policy. Thresholds are **integer build numbers** (Android `versionCode`, the iOS build)
     * — never version strings, which don't sort. `0` means no requirement.
     */
    data class UpdatePolicy(
        val minSupportedBuild: Long = 0,
        val recommendedBuild: Long = 0,
        val androidStoreUrl: String? = null,
        val iosStoreUrl: String? = null,
        /** Optional copy for either prompt. */
        val message: String? = null,
    ) {
        /**
         * What this policy asks of a given [buildNumber]. An unknown build (`<= 0`, e.g. the package
         * info couldn't be read) is never blocked — config must not lock the app out because of a
         * client-side hiccup.
         */
        fun requirementFor(buildNumber: Long): UpdateRequirement = when {
            buildNumber <= 0 -> UpdateRequirement.NONE
            minSupportedBuild > 0 && buildNumber < minSupportedBuild -> UpdateRequirement.REQUIRED
            recommendedBuild > 0 && buildNumber < recommendedBuild -> UpdateRequirement.RECOMMENDED
            else -> UpdateRequirement.NONE
        }

        /** The store listing for [platform], or null while the listings don't exist yet. */
        fun storeUrlFor(platform: AppPlatform): String? = when (platform) {
            AppPlatform.ANDROID -> androidStoreUrl
            AppPlatform.IOS -> iosStoreUrl
        }
    }

    /** `enabled` ⇒ the app shows [message] instead of loading anything. */
    data class Maintenance(
        val enabled: Boolean = false,
        val message: String? = null,
    )

    /**
     * What the server enforces, mirrored for pre-flight checks and copy — the server still enforces
     * every one of them.
     */
    data class Limits(
        /** Largest photo `POST /images` accepts. */
        val maxUploadBytes: Long = 8_388_608,
        /** The photo types `POST /images` accepts. */
        val uploadMimeTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"),
        /** Per-IP upload allowance — why photos upload as they're picked, not all at the end. */
        val uploadsPerHour: Int = 20,
        /** Drafts one principal may hold. */
        val maxDrafts: Int = 20,
        /** Photos one principal may hold. */
        val maxImages: Int = 60,
        /** How long a draft preview link (`d_…`) stays valid. */
        val draftPreviewMinutes: Int = 30,
    )

    /** Where the web renderer lives: template art, previews and share links resolve against [origin]. */
    data class Web(
        val origin: String = ApiConfig.WEB_ORIGIN,
    )

    /** Absolute URLs for the legal pages (HOA protocol 12), plus the support address. */
    data class Links(
        val privacyPolicy: String = "${ApiConfig.WEB_ORIGIN}/privacy",
        val terms: String = "${ApiConfig.WEB_ORIGIN}/terms",
        val deleteAccount: String = "${ApiConfig.WEB_ORIGIN}/delete-account",
        val supportEmail: String = "support@ferbotz.com",
    )

    companion object {
        /** The compiled-in defaults — what the app runs on until a config has ever been fetched. */
        val Default: RemoteConfig = RemoteConfig()
    }
}

/** How a [RemoteConfig.UpdatePolicy] applies to the running build. */
enum class UpdateRequirement {
    NONE,

    /** Below `recommendedBuild`: a dismissible "update available" nudge. */
    RECOMMENDED,

    /** Below `minSupportedBuild`: a hard "update to continue" block. */
    REQUIRED,
}

/**
 * A resolved config document plus its provenance: the [config] itself, the backend's metadata about
 * it, and when this device fetched it (`null` for the compiled-in defaults).
 */
data class RemoteConfigSnapshot(
    val schemaVersion: Int,
    val variant: String,
    val revision: String,
    val ttlSeconds: Long,
    val config: RemoteConfig,
    val fetchedAtEpochMillis: Long?,
    val source: Source,
) {
    enum class Source {
        /** Never fetched on this device — the compiled-in [RemoteConfig.Default]. */
        DEFAULTS,

        /** The last good document, restored from local storage at startup. */
        CACHE,

        /** Fetched from the backend during this process. */
        NETWORK,
    }

    /** True when the document is older than its [ttlSeconds], or was never fetched. */
    fun isStale(nowEpochMillis: Long): Boolean {
        val fetchedAt = fetchedAtEpochMillis ?: return true
        return nowEpochMillis - fetchedAt >= ttlSeconds * 1000
    }

    companion object {
        /** The schema this build was written against; the backend bumps it only on a removed/renamed key. */
        const val SUPPORTED_SCHEMA_VERSION: Int = 1

        /** Default TTL when the backend doesn't say (matches its current value). */
        const val DEFAULT_TTL_SECONDS: Long = 3600

        val Defaults: RemoteConfigSnapshot = RemoteConfigSnapshot(
            schemaVersion = SUPPORTED_SCHEMA_VERSION,
            variant = "default",
            revision = "",
            ttlSeconds = DEFAULT_TTL_SECONDS,
            config = RemoteConfig.Default,
            fetchedAtEpochMillis = null,
            source = Source.DEFAULTS,
        )
    }
}
