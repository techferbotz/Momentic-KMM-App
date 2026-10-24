package com.ferbotz.momentic.data.local.preferences

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Small key-value store for identity and the remote-config cache, backed by multiplatform-settings
 * (SharedPreferences on Android, NSUserDefaults on iOS).
 *
 * It holds five things:
 * - the **install id**, generated once and sent as `X-Device-Id` (remote config's bucketing key);
 * - the **draft token** from `POST /draft-sessions` — the anonymous device principal that owns
 *   everything made before sign-in;
 * - the **access token** once signed in. It never expires (HOA protocol 04): it is stored once and
 *   reused on every launch until the server rejects it;
 * - the last good **remote config** document, so the next launch starts on yesterday's config;
 * - whether the first-launch **welcome** has been seen.
 *
 * Only the access token belongs to the session. The install id, the draft token, the config and the
 * welcome flag are per device, so [clearSession] leaves them alone — signing out must not throw away a
 * kill switch, or show the welcome again.
 */
class AppPreferences(private val settings: Settings) {

    /**
     * A stable per-install id, generated and persisted on first access so the very first request
     * already carries one (HOA protocol 11 §1).
     */
    @OptIn(ExperimentalUuidApi::class)
    val deviceId: String
        get() = settings.getStringOrNull(KEY_DEVICE_ID) ?: Uuid.random().toString().also {
            settings[KEY_DEVICE_ID] = it
        }

    /** The anonymous device principal, sent as `X-Draft-Token` while signed out. */
    val draftToken: String?
        get() = settings.getStringOrNull(KEY_DRAFT_TOKEN)

    fun saveDraftToken(token: String) {
        settings[KEY_DRAFT_TOKEN] = token
    }

    /** Forgets a draft token the server no longer accepts; the next creation call mints a new one. */
    fun clearDraftToken() {
        settings.remove(KEY_DRAFT_TOKEN)
    }

    /** The access token, or null while anonymous. */
    val authToken: String?
        get() = settings.getStringOrNull(KEY_AUTH_TOKEN)

    val isSignedIn: Boolean
        get() = authToken != null

    fun saveAuthToken(token: String) {
        settings[KEY_AUTH_TOKEN] = token
    }

    /** The last profile seen for the signed-in account (JSON), so the account screen renders offline. */
    val cachedUserJson: String?
        get() = settings.getStringOrNull(KEY_USER)

    fun cacheUserJson(json: String) {
        settings[KEY_USER] = json
    }

    /**
     * Purchases paid for in the store whose publish or renewal the server hasn't confirmed yet (JSON).
     * Per device and kept across sign-outs: a paid purchase must never be dropped.
     */
    val pendingPurchasesJson: String?
        get() = settings.getStringOrNull(KEY_PENDING_PURCHASES)

    fun savePendingPurchasesJson(json: String?) {
        if (json == null) settings.remove(KEY_PENDING_PURCHASES) else settings[KEY_PENDING_PURCHASES] = json
    }

    /**
     * The last good `GET /config` payload (`data`, as JSON) and when it was fetched. Per device, not
     * per session — [clearSession] deliberately leaves it alone.
     */
    val remoteConfigJson: String?
        get() = settings.getStringOrNull(KEY_REMOTE_CONFIG)

    /** Epoch millis of the fetch that produced [remoteConfigJson], or null if never fetched. */
    val remoteConfigFetchedAt: Long?
        get() = settings.getLongOrNull(KEY_REMOTE_CONFIG_FETCHED_AT)

    fun cacheRemoteConfig(json: String, fetchedAtEpochMillis: Long) {
        settings[KEY_REMOTE_CONFIG] = json
        settings[KEY_REMOTE_CONFIG_FETCHED_AT] = fetchedAtEpochMillis
    }

    /** True once "Start creating" on the first-launch welcome has been tapped on this device. */
    val hasSeenWelcome: Boolean
        get() = settings.getBoolean(KEY_SEEN_WELCOME, false)

    fun markWelcomeSeen() {
        settings[KEY_SEEN_WELCOME] = true
    }

    /**
     * Ends the session: forgets the access token and the cached profile. Keeps the install id, the
     * draft token, the remote config, the welcome flag and any pending purchases, which belong to the
     * device rather than the account.
     */
    fun clearSession() {
        settings.remove(KEY_AUTH_TOKEN)
        settings.remove(KEY_USER)
    }

    private companion object {
        const val KEY_DEVICE_ID = "momentic.device_id"
        const val KEY_DRAFT_TOKEN = "momentic.draft_token"
        const val KEY_AUTH_TOKEN = "momentic.auth_token"
        const val KEY_USER = "momentic.user"
        const val KEY_PENDING_PURCHASES = "momentic.pending_purchases"
        const val KEY_REMOTE_CONFIG = "momentic.remote_config"
        const val KEY_REMOTE_CONFIG_FETCHED_AT = "momentic.remote_config_fetched_at"
        const val KEY_SEEN_WELCOME = "momentic.seen_welcome"
    }
}
