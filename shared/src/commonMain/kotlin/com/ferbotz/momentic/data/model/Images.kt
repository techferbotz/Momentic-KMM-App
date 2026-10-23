package com.ferbotz.momentic.data.model

/**
 * A photo as `POST /images` stored it: transcoded to WebP in two sizes, EXIF stripped. Creations
 * reference it by [id] only — `{ "imageId": … }` — never by URL; the URLs are for showing it in the app.
 */
data class UploadedImage(
    val id: String,
    /** ≤ 1280 px, WebP. */
    val fullUrl: String?,
    /** ≤ 512 px, WebP. */
    val thumbUrl: String?,
    val width: Int?,
    val height: Int?,
)

/**
 * A photo ready to upload: upright, cropped to its field's aspect ratio, scaled down and re-encoded
 * (see `ImageProcessor`). Not a data class: equality on the bytes would be by reference anyway.
 */
class PreparedImage(
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int,
    val height: Int,
) {
    /** A file name for the multipart part; the server only looks at the bytes and the type. */
    val fileName: String
        get() = when (mimeType) {
            "image/png" -> "photo.png"
            "image/webp" -> "photo.webp"
            else -> "photo.jpg"
        }
}
