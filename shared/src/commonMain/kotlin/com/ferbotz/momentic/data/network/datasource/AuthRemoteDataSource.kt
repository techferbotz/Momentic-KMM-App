package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.AuthResponseDto
import com.ferbotz.momentic.data.network.dto.ClaimDraftsRequest
import com.ferbotz.momentic.data.network.dto.ClaimedDto
import com.ferbotz.momentic.data.network.dto.GoogleSignInRequest
import com.ferbotz.momentic.data.network.dto.UserDto
import com.ferbotz.momentic.data.network.ensureSuccess
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** Talks to the identity endpoints. Sign-in, refresh-free: one token, stored once (HOA protocol 04). */
class AuthRemoteDataSource(private val client: HttpClient) {

    /** `POST /auth/google` — `401 INVALID_GOOGLE_TOKEN` when Google's id token doesn't verify. */
    suspend fun signInWithGoogle(request: GoogleSignInRequest): AuthResponseDto =
        client.post(ApiConfig.Paths.AUTH_GOOGLE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.unwrap()

    /** `POST /auth/claim-drafts` — moves a draft session's work onto the signed-in user. */
    suspend fun claimDrafts(draftToken: String): ClaimedDto =
        client.post(ApiConfig.Paths.AUTH_CLAIM_DRAFTS) {
            contentType(ContentType.Application.Json)
            setBody(ClaimDraftsRequest(draftToken))
        }.unwrap()

    /** `GET /me`. The contract shows the user object; `{ user: … }` is accepted too. */
    suspend fun me(): UserDto {
        val data = client.get(ApiConfig.Paths.ME).unwrap<JsonObject>()
        return appJson.decodeFromJsonElement(data["user"] as? JsonObject ?: data)
    }

    /** `DELETE /me` — soft-deletes the account; its token is rejected from then on. */
    suspend fun deleteMe() {
        client.delete(ApiConfig.Paths.ME).ensureSuccess()
    }
}
