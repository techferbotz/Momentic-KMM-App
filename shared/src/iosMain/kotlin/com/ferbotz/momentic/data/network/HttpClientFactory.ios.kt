package com.ferbotz.momentic.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/** iOS uses the Darwin (NSURLSession) engine. */
actual fun httpClientEngine(): HttpClientEngine = Darwin.create()
