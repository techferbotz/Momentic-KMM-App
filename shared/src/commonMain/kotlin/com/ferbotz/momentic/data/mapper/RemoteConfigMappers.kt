package com.ferbotz.momentic.data.mapper

import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.network.dto.RemoteConfigDto
import com.ferbotz.momentic.data.network.dto.RemoteConfigResponseDto
import com.ferbotz.momentic.data.util.isWebUrl

/**
 * Resolves a wire document into a [RemoteConfigSnapshot], filling every missing key from [defaults]
 * — the contract's "ignore unknown keys, default missing ones" rule. (Unknown keys never reach here;
 * JSON decoding drops them.)
 */
fun RemoteConfigResponseDto.toDomain(
    fetchedAtEpochMillis: Long?,
    source: RemoteConfigSnapshot.Source,
    defaults: RemoteConfig = RemoteConfig.Default,
): RemoteConfigSnapshot {
    val base = RemoteConfigSnapshot.Defaults
    return RemoteConfigSnapshot(
        schemaVersion = schemaVersion ?: base.schemaVersion,
        variant = variant?.takeIf { it.isNotBlank() } ?: base.variant,
        revision = revision ?: base.revision,
        ttlSeconds = ttlSeconds?.takeIf { it > 0 } ?: base.ttlSeconds,
        config = config.toDomain(defaults),
        fetchedAtEpochMillis = fetchedAtEpochMillis,
        source = source,
    )
}

/**
 * Leaf-by-leaf merge of the wire document over [defaults]: a null section or leaf keeps the default.
 * Never merge section-wise (`features = dto.features ?: default`) — a partial section would then wipe
 * the leaves it didn't mention (HOA protocol 11 §2 rule 3).
 */
fun RemoteConfigDto?.toDomain(defaults: RemoteConfig = RemoteConfig.Default): RemoteConfig {
    if (this == null) return defaults
    val f = features
    val fd = defaults.features
    val u = update
    val ud = defaults.update
    val m = maintenance
    val md = defaults.maintenance
    val l = limits
    val ld = defaults.limits
    val w = web
    val wd = defaults.web
    val k = links
    val kd = defaults.links
    return RemoteConfig(
        features = RemoteConfig.Features(
            googleLogin = f?.googleLogin ?: fd.googleLogin,
            publishing = f?.publishing ?: fd.publishing,
            renewal = f?.renewal ?: fd.renewal,
            stats = f?.stats ?: fd.stats,
        ),
        update = RemoteConfig.UpdatePolicy(
            minSupportedBuild = u?.minSupportedBuild ?: ud.minSupportedBuild,
            recommendedBuild = u?.recommendedBuild ?: ud.recommendedBuild,
            androidStoreUrl = u?.androidStoreUrl ?: ud.androidStoreUrl,
            iosStoreUrl = u?.iosStoreUrl ?: ud.iosStoreUrl,
            message = u?.message ?: ud.message,
        ),
        maintenance = RemoteConfig.Maintenance(
            enabled = m?.enabled ?: md.enabled,
            message = m?.message ?: md.message,
        ),
        limits = RemoteConfig.Limits(
            // A limit of zero would make the feature unusable rather than off; treat it as absent.
            maxUploadBytes = l?.maxUploadBytes?.takeIf { it > 0 } ?: ld.maxUploadBytes,
            uploadMimeTypes = l?.uploadMimeTypes?.takeIf { it.isNotEmpty() } ?: ld.uploadMimeTypes,
            uploadsPerHour = l?.uploadsPerHour?.takeIf { it > 0 } ?: ld.uploadsPerHour,
            maxDrafts = l?.maxDrafts?.takeIf { it > 0 } ?: ld.maxDrafts,
            maxImages = l?.maxImages?.takeIf { it > 0 } ?: ld.maxImages,
            draftPreviewMinutes = l?.draftPreviewMinutes?.takeIf { it > 0 } ?: ld.draftPreviewMinutes,
        ),
        web = RemoteConfig.Web(
            // Every thumbnail and preview resolves against this, so only a real origin replaces the default.
            origin = w?.origin?.trim()?.trimEnd('/')?.takeIf(::isWebUrl) ?: wd.origin,
        ),
        links = RemoteConfig.Links(
            privacyPolicy = k?.privacyPolicy ?: kd.privacyPolicy,
            terms = k?.terms ?: kd.terms,
            deleteAccount = k?.deleteAccount ?: kd.deleteAccount,
            supportEmail = k?.supportEmail ?: kd.supportEmail,
        ),
    )
}
