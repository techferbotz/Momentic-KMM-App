package com.ferbotz.momentic

import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.model.UpdateRequirement
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.RemoteConfigResponseDto
import com.ferbotz.momentic.data.platform.AppPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The `GET /config` document resolves per the forward-compat rules (HOA protocol 11 §5): every key is
 * optional on the wire, unknown keys are ignored, missing keys take the compiled-in default — leaf
 * by leaf, not section by section.
 */
class RemoteConfigParsingTest {

    private fun parse(dataJson: String): RemoteConfigSnapshot =
        appJson.decodeFromString<RemoteConfigResponseDto>(dataJson)
            .toDomain(fetchedAtEpochMillis = 1_000L, source = RemoteConfigSnapshot.Source.NETWORK)

    /** A full document with values flipped away from the defaults. */
    private val fullDocument = """
        {
          "schemaVersion": 1,
          "variant": "default+legacy-android-publish-off",
          "revision": "a19f30a7181d",
          "ttlSeconds": 900,
          "config": {
            "features": { "googleLogin": false, "publishing": false, "renewal": true, "stats": false },
            "update": { "minSupportedBuild": 5, "recommendedBuild": 9, "androidStoreUrl": "https://play.example/momentic",
                        "iosStoreUrl": null, "message": "Please update" },
            "maintenance": { "enabled": true, "message": "Back in 10 minutes" },
            "limits": { "maxUploadBytes": 1048576, "uploadMimeTypes": ["image/jpeg"], "uploadsPerHour": 5,
                        "maxDrafts": 3, "maxImages": 9, "draftPreviewMinutes": 10 },
            "web": { "origin": "https://staging.example/" },
            "links": { "privacyPolicy": "https://x.example/privacy", "terms": "https://x.example/terms",
                       "deleteAccount": "https://x.example/delete", "supportEmail": "help@x.example" }
          }
        }
    """.trimIndent()

    /** `GET https://momentic.ferbotz.com/api/v1/config` as served on 2026-10-23. */
    private val productionDocument = """
        {"schemaVersion":1,"variant":"default","revision":"042123e5fea4","ttlSeconds":3600,"config":{
          "features":{"googleLogin":true,"publishing":true,"renewal":true,"stats":true},
          "update":{"minSupportedBuild":0,"recommendedBuild":0,"androidStoreUrl":null,"iosStoreUrl":null,"message":null},
          "maintenance":{"enabled":false,"message":null},
          "limits":{"maxUploadBytes":8388608,"uploadMimeTypes":["image/jpeg","image/png","image/webp","image/heic","image/heif"],
                    "uploadsPerHour":20,"maxDrafts":20,"maxImages":60,"draftPreviewMinutes":30},
          "web":{"origin":"https://momentic.ferbotz.com"},
          "links":{"privacyPolicy":"https://momentic.ferbotz.com/privacy","terms":"https://momentic.ferbotz.com/terms",
                   "deleteAccount":"https://momentic.ferbotz.com/delete-account","supportEmail":"support@ferbotz.com"}}}
    """.trimIndent()

    @Test
    fun fullDocumentMapsEveryField() {
        val snapshot = parse(fullDocument)
        assertEquals(1, snapshot.schemaVersion)
        assertEquals("default+legacy-android-publish-off", snapshot.variant)
        assertEquals("a19f30a7181d", snapshot.revision)
        assertEquals(900L, snapshot.ttlSeconds)
        assertEquals(RemoteConfigSnapshot.Source.NETWORK, snapshot.source)

        val config = snapshot.config
        assertFalse(config.features.googleLogin)
        assertFalse(config.features.publishing)
        assertTrue(config.features.renewal)
        assertFalse(config.features.stats)
        assertEquals(5L, config.update.minSupportedBuild)
        assertEquals(9L, config.update.recommendedBuild)
        assertEquals("https://play.example/momentic", config.update.androidStoreUrl)
        assertNull(config.update.iosStoreUrl)
        assertEquals("Please update", config.update.message)
        assertTrue(config.maintenance.enabled)
        assertEquals("Back in 10 minutes", config.maintenance.message)
        assertEquals(1_048_576L, config.limits.maxUploadBytes)
        assertEquals(listOf("image/jpeg"), config.limits.uploadMimeTypes)
        assertEquals(5, config.limits.uploadsPerHour)
        assertEquals(3, config.limits.maxDrafts)
        assertEquals(9, config.limits.maxImages)
        assertEquals(10, config.limits.draftPreviewMinutes)
        assertEquals("https://staging.example", config.web.origin, "a trailing slash is trimmed")
        assertEquals("https://x.example/terms", config.links.terms)
        assertEquals("help@x.example", config.links.supportEmail)
    }

    @Test
    fun productionDocumentEqualsTheCompiledInDefaults() {
        // HOA protocol 11 §2 rule 4: the defaults in code equal the backend's defaults, so adopting
        // the config changes nothing until a value is deliberately flipped.
        val snapshot = parse(productionDocument)
        assertEquals(RemoteConfig.Default, snapshot.config)
        assertEquals("042123e5fea4", snapshot.revision)
    }

    @Test
    fun emptyDataDocumentResolvesToCompiledInDefaults() {
        val snapshot = parse("{}")
        assertEquals(RemoteConfig.Default, snapshot.config)
        assertEquals(RemoteConfigSnapshot.SUPPORTED_SCHEMA_VERSION, snapshot.schemaVersion)
        assertEquals("default", snapshot.variant)
        assertEquals(RemoteConfigSnapshot.DEFAULT_TTL_SECONDS, snapshot.ttlSeconds)
        assertEquals(1_000L, snapshot.fetchedAtEpochMillis)
    }

    @Test
    fun emptyConfigObjectResolvesToCompiledInDefaults() {
        val snapshot = parse("""{ "schemaVersion": 1, "variant": "default", "revision": "abc", "ttlSeconds": 60, "config": {} }""")
        assertEquals(RemoteConfig.Default, snapshot.config)
        assertEquals("abc", snapshot.revision)
        assertEquals(60L, snapshot.ttlSeconds)
    }

    @Test
    fun partialSectionsDefaultLeafByLeaf() {
        val config = parse("""{ "config": { "features": { "publishing": false }, "maintenance": { "enabled": true } } }""").config
        assertFalse(config.features.publishing)
        assertTrue(config.features.googleLogin, "an unmentioned leaf keeps its default")
        assertTrue(config.features.renewal)
        assertTrue(config.features.stats)
        assertTrue(config.maintenance.enabled)
        assertNull(config.maintenance.message)
        assertEquals(RemoteConfig.Default.update, config.update, "an unmentioned section keeps its defaults")
        assertEquals(RemoteConfig.Default.limits, config.limits)
        assertEquals(RemoteConfig.Default.links, config.links)
    }

    @Test
    fun unknownKeysAreIgnoredAtEveryLevel() {
        val snapshot = parse(
            """
            { "schemaVersion": 2, "brandNewTopLevel": { "x": 1 },
              "config": { "features": { "publishing": true, "holograms": true }, "experiments": ["a", "b"],
                          "limits": { "maxVideoSeconds": 30 } } }
            """.trimIndent(),
        )
        assertEquals(2, snapshot.schemaVersion)
        assertEquals(RemoteConfig.Default, snapshot.config)
    }

    @Test
    fun explicitNullsAndUnusableValuesResolveLikeMissingKeys() {
        val config = parse(
            """
            { "config": { "update": { "androidStoreUrl": null, "message": null },
                          "limits": { "maxDrafts": null, "maxUploadBytes": 0, "uploadMimeTypes": [] },
                          "web": { "origin": "not a url" } } }
            """.trimIndent(),
        ).config
        assertNull(config.update.androidStoreUrl)
        assertNull(config.update.message)
        assertEquals(20, config.limits.maxDrafts)
        assertEquals(8_388_608L, config.limits.maxUploadBytes, "a zero cap falls back rather than blocking every upload")
        assertEquals(RemoteConfig.Default.limits.uploadMimeTypes, config.limits.uploadMimeTypes)
        assertEquals("https://momentic.ferbotz.com", config.web.origin, "only a real origin replaces the default")
    }

    @Test
    fun nonPositiveTtlAndBlankVariantFallBack() {
        val snapshot = parse("""{ "ttlSeconds": 0, "variant": "" }""")
        assertEquals(RemoteConfigSnapshot.DEFAULT_TTL_SECONDS, snapshot.ttlSeconds)
        assertEquals("default", snapshot.variant)
    }

    @Test
    fun updateRequirementComparesIntegerBuilds() {
        val policy = RemoteConfig.UpdatePolicy(minSupportedBuild = 10, recommendedBuild = 20, androidStoreUrl = "https://play.example")
        assertEquals(UpdateRequirement.REQUIRED, policy.requirementFor(9))
        assertEquals(UpdateRequirement.RECOMMENDED, policy.requirementFor(10))
        assertEquals(UpdateRequirement.RECOMMENDED, policy.requirementFor(19))
        assertEquals(UpdateRequirement.NONE, policy.requirementFor(20))
        assertEquals(UpdateRequirement.NONE, policy.requirementFor(0), "an unknown build is never blocked")
        assertEquals(UpdateRequirement.NONE, RemoteConfig.UpdatePolicy().requirementFor(1), "0 = no requirement")
        assertEquals("https://play.example", policy.storeUrlFor(AppPlatform.ANDROID))
        assertNull(policy.storeUrlFor(AppPlatform.IOS))
    }

    @Test
    fun snapshotStalenessFollowsTtl() {
        val snapshot = parse("""{ "ttlSeconds": 60 }""") // fetched at t = 1_000 ms
        assertFalse(snapshot.isStale(nowEpochMillis = 1_000L + 59_999L))
        assertTrue(snapshot.isStale(nowEpochMillis = 1_000L + 60_000L))
        assertTrue(RemoteConfigSnapshot.Defaults.isStale(nowEpochMillis = 0L), "never fetched ⇒ stale")
    }
}
