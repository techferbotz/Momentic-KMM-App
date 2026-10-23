package com.ferbotz.momentic.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/** Android uses the OkHttp engine. */
actual fun httpClientEngine(): HttpClientEngine = OkHttp.create()
