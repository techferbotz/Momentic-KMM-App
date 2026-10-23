package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.model.OpenGraph
import com.ferbotz.momentic.data.model.RenderMode
import com.ferbotz.momentic.data.model.RenderedPage
import com.ferbotz.momentic.data.network.datasource.RenderRemoteDataSource
import com.ferbotz.momentic.data.network.dto.RenderDto
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching
import com.ferbotz.momentic.data.util.parseInstantOrNull
import com.ferbotz.momentic.data.util.resolveWebUrl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The public render endpoint, as far as the app needs it. */
interface RenderRepository {

    suspend fun render(code: String): AppResult<RenderedPage>

    /**
     * A template's sample page (`pv_<templateId>`) — what the editor shows as example values. Kept in
     * memory for the life of the process; the backend lets preview renders be cached too.
     */
    suspend fun examplePage(previewCode: String): AppResult<RenderedPage>
}

/** @param webOrigin the current web origin: `og.imageUrl` and sample images are web paths. */
class RenderRepositoryImpl(
    private val remote: RenderRemoteDataSource,
    private val webOrigin: () -> String,
) : RenderRepository {

    private val examples = mutableMapOf<String, RenderedPage>()
    private val examplesLock = Mutex()

    override suspend fun render(code: String): AppResult<RenderedPage> =
        runApiCatching { remote.render(code).toDomain(code) }

    override suspend fun examplePage(previewCode: String): AppResult<RenderedPage> {
        examplesLock.withLock { examples[previewCode] }?.let { return AppResult.Success(it) }
        return render(previewCode).also { result ->
            if (result is AppResult.Success) examplesLock.withLock { examples[previewCode] = result.data }
        }
    }

    private fun RenderDto.toDomain(code: String): RenderedPage = RenderedPage(
        templateId = templateId ?: code.removePrefix("pv_"),
        values = values,
        mode = RenderMode.fromWire(meta?.mode),
        expiresAt = parseInstantOrNull(meta?.expiresAt),
        showBranding = meta?.showBranding ?: true,
        og = OpenGraph(
            title = og?.title.orEmpty(),
            description = og?.description.orEmpty(),
            imageUrl = resolveWebUrl(webOrigin(), og?.imageUrl),
        ),
        rsvpEnabled = features?.rsvp ?: false,
        creationId = creationId,
    )
}
