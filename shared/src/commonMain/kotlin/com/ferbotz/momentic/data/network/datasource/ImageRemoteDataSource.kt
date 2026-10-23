package com.ferbotz.momentic.data.network.datasource

import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.dto.UploadedImageDto
import com.ferbotz.momentic.data.network.ensureSuccess
import com.ferbotz.momentic.data.network.unwrap
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Talks to `/images` (device or user). `503 STORAGE_UNAVAILABLE` when the backend's storage is
 * unconfigured; uploads are rate-limited to 20 an hour per IP.
 */
class ImageRemoteDataSource(private val client: HttpClient) {

    /**
     * `POST /images` — `multipart/form-data` with a single field **`file`**, ≤ 8 MB, images only. The
     * server transcodes it to WebP in two sizes and strips EXIF.
     */
    suspend fun upload(bytes: ByteArray, mimeType: String, fileName: String): UploadedImageDto =
        client.submitFormWithBinaryData(
            url = ApiConfig.Paths.IMAGES,
            formData = formData {
                append(
                    key = "file",
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    },
                )
            },
        ) {
            // A few megabytes over a mobile connection outlasts the client's 30 s default.
            timeout {
                requestTimeoutMillis = UPLOAD_TIMEOUT_MILLIS
                socketTimeoutMillis = UPLOAD_TIMEOUT_MILLIS
            }
        }.unwrap()

    suspend fun delete(imageId: String) {
        client.delete(ApiConfig.Paths.image(imageId)).ensureSuccess()
    }

    private companion object {
        const val UPLOAD_TIMEOUT_MILLIS = 120_000L
    }
}
