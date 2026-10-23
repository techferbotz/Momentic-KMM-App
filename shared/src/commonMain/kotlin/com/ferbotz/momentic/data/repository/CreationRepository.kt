package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.Page
import com.ferbotz.momentic.data.model.PreviewLink
import com.ferbotz.momentic.data.model.Pricing
import com.ferbotz.momentic.data.model.RsvpPage
import com.ferbotz.momentic.data.result.AppResult
import kotlinx.serialization.json.JsonObject

/**
 * The user's creations (`API.md` § Creations). Everything here works for the anonymous device and the
 * signed-in user alike; the principal is handled underneath (see [PrincipalGuard]).
 *
 * Limits: 20 drafts and 60 photos per principal (`DRAFT_LIMIT_REACHED`, `IMAGE_LIMIT_REACHED`).
 */
interface CreationRepository {

    /** `POST /creations`. [data] must satisfy the template's field contract. */
    suspend fun create(templateId: String, data: JsonObject, coverImageId: String?): AppResult<Creation>

    /** `PUT /creations/:id` with the writable fields. */
    suspend fun update(creationId: String, data: JsonObject, coverImageId: String?): AppResult<Creation>

    suspend fun get(creationId: String): AppResult<Creation>

    /** One page, newest first. Pass `null` for the first page, then the previous page's cursor. */
    suspend fun list(cursor: String? = null, limit: Int = DEFAULT_PAGE_SIZE): AppResult<Page<Creation>>

    /** Soft-deletes the creation and releases its share code. */
    suspend fun delete(creationId: String): AppResult<Unit>

    /**
     * A fresh link to the creator's own draft on the web. Mint one each time it is opened; it dies
     * about 30 minutes after it is minted.
     */
    suspend fun previewLink(creationId: String): AppResult<PreviewLink>

    /** The bundles this creation can be published for. */
    suspend fun pricing(creationId: String): AppResult<Pricing>

    suspend fun stats(creationId: String): AppResult<CreationStats>

    suspend fun rsvps(creationId: String, cursor: String? = null, limit: Int = DEFAULT_PAGE_SIZE): AppResult<RsvpPage>

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
