package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.network.UnreadableResponseException
import com.ferbotz.momentic.data.network.datasource.CatalogueRemoteDataSource
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * @param webOrigin the current web origin (remote config `web.origin`), read at map time: template art
 *   resolves against it, never against the API base.
 */
class CatalogueRepositoryImpl(
    private val remote: CatalogueRemoteDataSource,
    private val webOrigin: () -> String,
) : CatalogueRepository {

    private val templates = mutableMapOf<String, TemplateDetail>()
    private val templatesLock = Mutex()

    override suspend fun getFeed(): AppResult<List<FeedSection>> = runApiCatching {
        remote.getFeed().toDomain(webOrigin())
    }

    override suspend fun getTemplate(templateId: String): AppResult<TemplateDetail> {
        templatesLock.withLock { templates[templateId] }?.let { return AppResult.Success(it) }
        return runApiCatching {
            val detail = remote.getTemplate(templateId).toDomain(webOrigin())
                ?: throw UnreadableResponseException(200, IllegalStateException("Template $templateId has no id or name"))
            templatesLock.withLock { templates[templateId] = detail }
            detail
        }
    }
}
