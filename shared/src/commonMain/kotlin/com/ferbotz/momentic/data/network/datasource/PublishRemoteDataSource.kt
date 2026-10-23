package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.PublishRequest
import com.ferbotz.momentic.data.network.dto.PublishResponseDto
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Talks to publish and renew — both require a **user**, not a device; 20 an hour per IP. */
class PublishRemoteDataSource(private val client: HttpClient) {

    /** `POST /creations/:id/publish` — assigns the share code and expiry once the purchase verifies. */
    suspend fun publish(creationId: String, storeTransactionId: String): PublishResponseDto =
        client.post(ApiConfig.Paths.publish(creationId)) {
            contentType(ContentType.Application.Json)
            setBody(PublishRequest(storeTransactionId))
        }.unwrap()

    /** `POST /creations/:id/renew` — extends the same link; renewing early adds to the time left. */
    suspend fun renew(creationId: String, storeTransactionId: String): PublishResponseDto =
        client.post(ApiConfig.Paths.renew(creationId)) {
            contentType(ContentType.Application.Json)
            setBody(PublishRequest(storeTransactionId))
        }.unwrap()
}
