package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.DraftSessionDto
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.post

/** Talks to `POST /draft-sessions` — no auth; rate-limited to 10 an hour per IP. */
class DraftSessionRemoteDataSource(private val client: HttpClient) {

    suspend fun createDraftSession(): DraftSessionDto =
        client.post(ApiConfig.Paths.DRAFT_SESSIONS).unwrap()
}
