package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.RemoteConfigResponseDto
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.get

/** Talks to `GET /api/v1/config` — no auth required; identity headers are used when present, never demanded. */
class RemoteConfigRemoteDataSource(private val client: HttpClient) {

    /** `GET /config` — the remote config document resolved for this device/build. */
    suspend fun getConfig(): RemoteConfigResponseDto =
        client.get(ApiConfig.Paths.CONFIG).unwrap()
}
