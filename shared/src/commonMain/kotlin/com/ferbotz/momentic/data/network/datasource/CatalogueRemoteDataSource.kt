package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.CategoriesDto
import com.ferbotz.momentic.data.network.dto.FeedDto
import com.ferbotz.momentic.data.network.dto.TemplateDetailDto
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.request.get

/** Talks to the catalogue endpoints — public, no principal needed. */
class CatalogueRemoteDataSource(private val client: HttpClient) {

    /** `GET /feed` — live categories, each with its templates. */
    suspend fun getFeed(): FeedDto =
        client.get(ApiConfig.Paths.FEED).unwrap()

    /** `GET /categories`. */
    suspend fun getCategories(): CategoriesDto =
        client.get(ApiConfig.Paths.CATEGORIES).unwrap()

    /** `GET /templates/:id` — the card plus the field contract and pricing. */
    suspend fun getTemplate(templateId: String): TemplateDetailDto =
        client.get(ApiConfig.Paths.template(templateId)).unwrap()
}
