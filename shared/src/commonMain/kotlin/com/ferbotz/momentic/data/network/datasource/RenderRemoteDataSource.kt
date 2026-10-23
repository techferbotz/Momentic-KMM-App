package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.RenderDto
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.get

/** Talks to the public render endpoint — no auth; 60 a minute per IP. */
class RenderRemoteDataSource(private val client: HttpClient) {

    /** `GET /public/render/{code}`. An expired link answers 200 with `meta.mode: "expired"`, not 404. */
    suspend fun render(code: String): RenderDto =
        client.get(ApiConfig.Paths.render(code)).unwrap()
}
