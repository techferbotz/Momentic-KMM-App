package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.mapper.toRsvpPage
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.Page
import com.ferbotz.momentic.data.model.PreviewLink
import com.ferbotz.momentic.data.model.Pricing
import com.ferbotz.momentic.data.model.RsvpPage
import com.ferbotz.momentic.data.network.UnreadableResponseException
import com.ferbotz.momentic.data.network.datasource.CreationRemoteDataSource
import com.ferbotz.momentic.data.network.dto.CreateCreationRequest
import com.ferbotz.momentic.data.network.dto.CreationDto
import com.ferbotz.momentic.data.network.dto.UpdateCreationRequest
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching
import com.ferbotz.momentic.data.util.parseInstantOrNull
import com.ferbotz.momentic.data.util.resolveWebUrl
import kotlinx.serialization.json.JsonObject

/**
 * @param webOrigin the current web origin (remote config `web.origin`): draft previews open at
 *   `<origin>/<code>`.
 */
class CreationRepositoryImpl(
    private val remote: CreationRemoteDataSource,
    private val guard: PrincipalGuard,
    private val events: CreationEvents,
    private val webOrigin: () -> String,
) : CreationRepository {

    override suspend fun create(templateId: String, data: JsonObject, coverImageId: String?): AppResult<Creation> =
        guard.run {
            runApiCatching {
                remote.create(CreateCreationRequest(templateId, data, coverImageId)).requireDomain()
            }
        }.also { result -> if (result is AppResult.Success) events.publish(CreationChange.Saved(result.data)) }

    override suspend fun update(creationId: String, data: JsonObject, coverImageId: String?): AppResult<Creation> =
        guard.run {
            runApiCatching { remote.update(creationId, UpdateCreationRequest(data, coverImageId)).requireDomain() }
        }.also { result -> if (result is AppResult.Success) events.publish(CreationChange.Saved(result.data)) }

    override suspend fun get(creationId: String): AppResult<Creation> =
        guard.run { runApiCatching { remote.get(creationId).requireDomain() } }

    override suspend fun list(cursor: String?, limit: Int): AppResult<Page<Creation>> =
        guard.run { runApiCatching { remote.list(cursor, limit).toDomain() } }

    override suspend fun delete(creationId: String): AppResult<Unit> =
        guard.run { runApiCatching { remote.delete(creationId) } }
            .also { result -> if (result is AppResult.Success) events.publish(CreationChange.Deleted(creationId)) }

    override suspend fun previewLink(creationId: String): AppResult<PreviewLink> =
        guard.run {
            runApiCatching {
                val token = remote.previewToken(creationId)
                val url = resolveWebUrl(webOrigin(), token.code)
                    ?: throw UnreadableResponseException(200, IllegalStateException("Empty preview code"))
                PreviewLink(code = token.code, url = url, expiresAt = parseInstantOrNull(token.expiresAt))
            }
        }

    override suspend fun pricing(creationId: String): AppResult<Pricing> =
        guard.run { runApiCatching { remote.pricing(creationId).toDomain() } }

    override suspend fun stats(creationId: String): AppResult<CreationStats> =
        guard.run { runApiCatching { remote.stats(creationId).toDomain() } }

    override suspend fun rsvps(creationId: String, cursor: String?, limit: Int): AppResult<RsvpPage> =
        guard.run { runApiCatching { remote.rsvps(creationId, cursor, limit).toRsvpPage() } }

    private fun CreationDto.requireDomain(): Creation =
        toDomain() ?: throw UnreadableResponseException(200, IllegalStateException("Creation without an id"))
}
