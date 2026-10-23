package com.ferbotz.momentic.data.network.dto

import kotlinx.serialization.Serializable

/**
 * `data` payload of `GET /api/v1/config` (HOA protocol 10; the backend's `docs/REMOTE_CONFIG.md`).
 *
 * Every field is nullable with a `null` default on purpose: the document is **forward-compatible on
 * the wire**. A key this build doesn't know is dropped by `appJson.ignoreUnknownKeys`; a key the
 * backend hasn't sent parses as `null`, and the mapper (`RemoteConfigMappers.kt`) fills it from the
 * compiled-in defaults, leaf by leaf. Only a removed or renamed key bumps [schemaVersion].
 */
@Serializable
data class RemoteConfigResponseDto(
    val schemaVersion: Int? = null,
    /** Which resolution this device got — `"default"`, or `"default+<ruleId>+…"` once targeting rules exist. */
    val variant: String? = null,
    /** Fingerprint of [config]; changes whenever any value changes. */
    val revision: String? = null,
    /** How long the document may be used before re-fetching. */
    val ttlSeconds: Long? = null,
    val config: RemoteConfigDto? = null,
)

/** The config document itself. Sections and leaves are all optional — see [RemoteConfigResponseDto]. */
@Serializable
data class RemoteConfigDto(
    val features: FeaturesDto? = null,
    val update: UpdatePolicyDto? = null,
    val maintenance: MaintenanceDto? = null,
    val limits: LimitsDto? = null,
    val web: WebDto? = null,
    val links: LinksDto? = null,
)

/** Client-side kill switches for UI entry points; the API routes keep working regardless. */
@Serializable
data class FeaturesDto(
    val googleLogin: Boolean? = null,
    val publishing: Boolean? = null,
    val renewal: Boolean? = null,
    val stats: Boolean? = null,
)

/** Update policy, compared against the **integer** build number (never the version string). `0` = no requirement. */
@Serializable
data class UpdatePolicyDto(
    val minSupportedBuild: Long? = null,
    val recommendedBuild: Long? = null,
    val androidStoreUrl: String? = null,
    val iosStoreUrl: String? = null,
    val message: String? = null,
)

@Serializable
data class MaintenanceDto(
    val enabled: Boolean? = null,
    val message: String? = null,
)

/** Momentic's limits — mirrored from what the server enforces, for pre-flight checks and copy. */
@Serializable
data class LimitsDto(
    val maxUploadBytes: Long? = null,
    val uploadMimeTypes: List<String>? = null,
    val uploadsPerHour: Int? = null,
    val maxDrafts: Int? = null,
    val maxImages: Int? = null,
    val draftPreviewMinutes: Int? = null,
)

/** Momentic's web section: the origin template art, previews and share links live on. */
@Serializable
data class WebDto(
    val origin: String? = null,
)

@Serializable
data class LinksDto(
    val privacyPolicy: String? = null,
    val terms: String? = null,
    val deleteAccount: String? = null,
    val supportEmail: String? = null,
)
