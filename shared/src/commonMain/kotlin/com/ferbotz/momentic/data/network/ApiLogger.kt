package com.ferbotz.momentic.data.network

import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.plugins.logging.Logger as KtorLogger

/**
 * Shared Kermit logger for the data layer (tag `Momentic-API`). Kermit's default logger comes with
 * a platform writer, so this prints to Logcat on Android and os_log on iOS with no extra setup.
 */
val apiLogger: KermitLogger = KermitLogger.withTag("Momentic-API")

/**
 * Adapts Ktor's HTTP-client `Logging` plugin to Kermit: every request/response line the plugin
 * produces goes through [apiLogger] at debug severity.
 */
internal object KermitKtorLogger : KtorLogger {
    override fun log(message: String) {
        apiLogger.d { message }
    }
}
