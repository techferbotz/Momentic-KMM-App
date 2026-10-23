package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.mapper.toCreationDto
import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.CreateCreationRequest
import com.ferbotz.momentic.data.network.dto.CreationDto
import com.ferbotz.momentic.data.network.dto.CreationsPageDto
import com.ferbotz.momentic.data.network.dto.PreviewTokenDto
import com.ferbotz.momentic.data.network.dto.PricingDto
import com.ferbotz.momentic.data.network.dto.StatsDto
import com.ferbotz.momentic.data.network.dto.UpdateCreationRequest
import com.ferbotz.momentic.data.network.ensureSuccess
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

/**
 * Talks to `/creations` — scoped to the calling principal (the device's draft token, or the signed-in
 * user). A row belonging to someone else is reported as `404`, never `403`.
 */
class CreationRemoteDataSource(private val client: HttpClient) {

    /** `POST /creations` → 201 with the creation. */
    suspend fun create(request: CreateCreationRequest): CreationDto =
        client.post(ApiConfig.Paths.CREATIONS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.unwrap<JsonObject>().toCreationDto()

    /** `GET /creations?cursor=&limit=` — newest first. */
    suspend fun list(cursor: String?, limit: Int): CreationsPageDto =
        client.get(ApiConfig.Paths.CREATIONS) {
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
        }.unwrap()

    suspend fun get(id: String): CreationDto =
        client.get(ApiConfig.Paths.creation(id)).unwrap<JsonObject>().toCreationDto()

    /** `PUT /creations/:id` — server-owned fields are ignored if sent, so only the writable ones go. */
    suspend fun update(id: String, request: UpdateCreationRequest): CreationDto =
        client.put(ApiConfig.Paths.creation(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.unwrap<JsonObject>().toCreationDto()

    /** `DELETE /creations/:id` — a soft delete that also releases the share code. */
    suspend fun delete(id: String) {
        client.delete(ApiConfig.Paths.creation(id)).ensureSuccess()
    }

    /** `POST /creations/:id/preview-token` → a `d_…` code, valid about 30 minutes. */
    suspend fun previewToken(id: String): PreviewTokenDto =
        client.post(ApiConfig.Paths.previewToken(id)).unwrap()

    /** `GET /creations/:id/pricing` — the bundles for this creation's tier. */
    suspend fun pricing(id: String): PricingDto =
        client.get(ApiConfig.Paths.pricing(id)).unwrap()

    suspend fun stats(id: String): StatsDto =
        client.get(ApiConfig.Paths.stats(id)).unwrap()

    /** `GET /creations/:id/rsvps?cursor=&limit=` — read leniently; see `toRsvpPage`. */
    suspend fun rsvps(id: String, cursor: String?, limit: Int): JsonObject =
        client.get(ApiConfig.Paths.rsvps(id)) {
            cursor?.let { parameter("cursor", it) }
            parameter("limit", limit)
        }.unwrap()
}
