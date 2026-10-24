package com.ferbotz.momentic

import com.ferbotz.momentic.data.media.ImageProcessor
import com.ferbotz.momentic.data.media.PhotoFraming
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.CreationStatus
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.Page
import com.ferbotz.momentic.data.model.PreparedImage
import com.ferbotz.momentic.data.model.PreviewLink
import com.ferbotz.momentic.data.model.Pricing
import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.model.RenderedPage
import com.ferbotz.momentic.data.model.RsvpPage
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.model.UploadedImage
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.repository.ImageRepository
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.repository.RenderRepository
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject

/** Test doubles for the repositories, recording what the view models ask of them. */

class FakeRemoteConfig(config: RemoteConfig = RemoteConfig.Default) : RemoteConfigRepository {
    override val snapshot = MutableStateFlow(RemoteConfigSnapshot.Defaults.copy(config = config))
    override suspend fun refresh(): AppResult<RemoteConfigSnapshot> = AppResult.Success(snapshot.value)
    override suspend fun refreshIfStale(): AppResult<RemoteConfigSnapshot> = AppResult.Success(snapshot.value)
}

class FakeCatalogue(private val template: TemplateDetail) : CatalogueRepository {
    override suspend fun getFeed(): AppResult<List<FeedSection>> = AppResult.Success(emptyList())
    override suspend fun getTemplate(templateId: String): AppResult<TemplateDetail> = AppResult.Success(template)
}

fun creation(
    id: String,
    templateId: String,
    data: JsonObject,
    status: CreationStatus = CreationStatus.DRAFT,
): Creation = Creation(
    id = id,
    templateId = templateId,
    templateName = "",
    status = status,
    data = data,
    coverImageId = null,
    shareCode = null,
    shareUrl = null,
    publishedAt = null,
    expiresAt = null,
    isExpired = false,
    viewCount = 0,
    createdAt = null,
    updatedAt = null,
)

class FakeCreations : CreationRepository {
    /** Each create/update: the data sent and the cover image id. */
    val created = mutableListOf<Pair<JsonObject, String?>>()
    val updated = mutableListOf<Pair<JsonObject, String?>>()
    val deleted = mutableListOf<String>()
    var stored: Creation? = null

    private val unavailable = AppResult.Failure(AppError.Api(503, ErrorCode.INTERNAL, null))

    override suspend fun create(templateId: String, data: JsonObject, coverImageId: String?): AppResult<Creation> {
        created += data to coverImageId
        return AppResult.Success(creation("c${created.size}", templateId, data).also { stored = it })
    }

    override suspend fun update(creationId: String, data: JsonObject, coverImageId: String?): AppResult<Creation> {
        updated += data to coverImageId
        return AppResult.Success(creation(creationId, stored?.templateId.orEmpty(), data).also { stored = it })
    }

    override suspend fun get(creationId: String): AppResult<Creation> =
        stored?.takeIf { it.id == creationId }?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.Api(404, ErrorCode.NOT_FOUND, null))

    override suspend fun list(cursor: String?, limit: Int): AppResult<Page<Creation>> =
        AppResult.Success(Page(listOfNotNull(stored), null))

    override suspend fun delete(creationId: String): AppResult<Unit> {
        deleted += creationId
        return AppResult.Success(Unit)
    }

    override suspend fun previewLink(creationId: String): AppResult<PreviewLink> =
        AppResult.Success(PreviewLink("d_test", "https://momentic.ferbotz.com/d_test", null))

    override suspend fun pricing(creationId: String): AppResult<Pricing> = unavailable
    override suspend fun stats(creationId: String): AppResult<CreationStats> = unavailable
    override suspend fun rsvps(creationId: String, cursor: String?, limit: Int): AppResult<RsvpPage> = unavailable
}

class FakeImages : ImageRepository {
    var uploads = 0
    val deleted = mutableListOf<String>()

    /** The next upload fails with this, once. */
    var failNext: AppError? = null

    override suspend fun upload(image: PreparedImage): AppResult<UploadedImage> {
        failNext?.let { error ->
            failNext = null
            return AppResult.Failure(error)
        }
        uploads++
        return AppResult.Success(UploadedImage("img$uploads", "https://cdn.test/full$uploads.webp", "https://cdn.test/thumb$uploads.webp", 10, 10))
    }

    override suspend fun delete(imageId: String): AppResult<Unit> {
        deleted += imageId
        return AppResult.Success(Unit)
    }
}

/** Passes the bytes straight through, remembering the crop hint and the framing it was asked for. */
class FakeProcessor : ImageProcessor {
    val aspectRatios = mutableListOf<AspectRatio?>()
    val framings = mutableListOf<PhotoFraming?>()

    override suspend fun prepare(
        source: ByteArray,
        aspectRatio: AspectRatio?,
        maxBytes: Long,
        maxDimension: Int,
        framing: PhotoFraming?,
    ): AppResult<PreparedImage> {
        aspectRatios += aspectRatio
        framings += framing
        return AppResult.Success(PreparedImage(source, "image/jpeg", 10, 10))
    }
}

/** No sample page and no draft render: the editor must cope without either. */
class FakeRender : RenderRepository {
    override suspend fun render(code: String): AppResult<RenderedPage> = AppResult.Failure(AppError.Network())
    override suspend fun examplePage(previewCode: String): AppResult<RenderedPage> = AppResult.Failure(AppError.Network())
}
