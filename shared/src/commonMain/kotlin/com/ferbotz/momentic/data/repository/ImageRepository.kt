package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.model.PreparedImage
import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.UploadedImage
import com.ferbotz.momentic.data.network.UnreadableResponseException
import com.ferbotz.momentic.data.network.datasource.ImageRemoteDataSource
import com.ferbotz.momentic.data.network.dto.UploadedImageDto
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching

/**
 * Photo uploads. Upload each photo as the creator picks it rather than all at the end: uploads are
 * limited to 20 an hour per IP, and a template taking eight photos would burn nearly half of that in
 * one go (`API.md` § Rate limits).
 */
interface ImageRepository {

    /** Checks [image] against the limits the server enforces, then uploads it. */
    suspend fun upload(image: PreparedImage): AppResult<UploadedImage>

    /** Frees a photo nothing uses any more (it counts towards the 60-photo limit until then). */
    suspend fun delete(imageId: String): AppResult<Unit>
}

/**
 * @param limits the current `limits.*` from remote config — mirrored from what the server enforces,
 *   so an oversized or unsupported photo is refused before it costs an upload.
 */
class ImageRepositoryImpl(
    private val remote: ImageRemoteDataSource,
    private val guard: PrincipalGuard,
    private val limits: () -> RemoteConfig.Limits,
) : ImageRepository {

    override suspend fun upload(image: PreparedImage): AppResult<UploadedImage> {
        val current = limits()
        if (image.bytes.size.toLong() > current.maxUploadBytes) {
            return AppResult.Failure(
                AppError.Invalid("That photo is too big (over ${megabytes(current.maxUploadBytes)}). Try a smaller one."),
            )
        }
        if (current.uploadMimeTypes.none { it.equals(image.mimeType, ignoreCase = true) }) {
            return AppResult.Failure(AppError.Invalid("That kind of file can't be used here. Pick a photo instead."))
        }
        return guard.run {
            runApiCatching { remote.upload(image.bytes, image.mimeType, image.fileName).toDomain() }
        }
    }

    override suspend fun delete(imageId: String): AppResult<Unit> =
        guard.run { runApiCatching { remote.delete(imageId) } }

    private fun UploadedImageDto.toDomain(): UploadedImage {
        val id = id?.takeIf { it.isNotBlank() }
            ?: throw UnreadableResponseException(201, IllegalStateException("Uploaded image without an id"))
        return UploadedImage(id = id, fullUrl = full, thumbUrl = thumb, width = width, height = height)
    }

    private fun megabytes(bytes: Long): String {
        val tenths = (bytes * 10) / (1024L * 1024L)
        return if (tenths % 10 == 0L) "${tenths / 10} MB" else "${tenths / 10}.${tenths % 10} MB"
    }
}
