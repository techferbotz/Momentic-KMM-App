package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.result.AppResult

/**
 * The template catalogue. Network-first: the feed is fetched fresh each time it is asked for, so a
 * template the backend just made live shows up on the next pull. Template details are kept in memory
 * for the life of the process, so the template screen and the editor share one fetch.
 */
interface CatalogueRepository {

    suspend fun getFeed(): AppResult<List<FeedSection>>

    /** The template, from memory when this process has already fetched it. */
    suspend fun getTemplate(templateId: String): AppResult<TemplateDetail>
}
