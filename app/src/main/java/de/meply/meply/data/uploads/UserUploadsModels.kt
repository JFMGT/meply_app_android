package de.meply.meply.data.uploads

import com.google.gson.annotations.SerializedName
import de.meply.meply.network.ApiClient

/**
 * Response from /user-uploads/me (Strapi format)
 * Lists all images uploaded by the current user
 */
data class UserUploadsResponse(
    @SerializedName("data") val data: List<UserUpload>?,
    @SerializedName("meta") val meta: UploadsMeta?
) {
    // Helper to get results (for compatibility)
    val results: List<UserUpload>?
        get() = data

    // Helper to get pagination (for compatibility)
    val pagination: UploadsPagination?
        get() = meta?.pagination
}

data class UploadsMeta(
    @SerializedName("pagination") val pagination: UploadsPagination?
)

data class UploadsPagination(
    @SerializedName("page") val page: Int?,
    @SerializedName("pageSize") val pageSize: Int?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("total") val total: Int?
)

/**
 * A single user upload entry
 */
data class UserUpload(
    @SerializedName("id") val id: Int,
    @SerializedName("reason") val reason: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("file") val file: UploadedFile?
)

/**
 * File details for an uploaded image (Strapi format)
 */
data class UploadedFile(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("ext") val ext: String?,
    @SerializedName("mime") val mime: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("formats") val formats: ImageFormats?
) {
    /**
     * Get full URL for the image
     */
    fun getFullUrl(): String? {
        return url?.let { ApiClient.STRAPI_IMAGE_BASE + it }
    }

    /**
     * Get thumbnail URL (from formats or fallback to main url)
     */
    fun getThumbnailUrl(): String? {
        val thumbPath = formats?.thumbnail?.url ?: url
        return thumbPath?.let { ApiClient.STRAPI_IMAGE_BASE + it }
    }

    /**
     * Get preview URL (medium > small > main url)
     */
    fun getPreviewUrl(): String? {
        val previewPath = formats?.medium?.url
            ?: formats?.small?.url
            ?: url
        return previewPath?.let { ApiClient.STRAPI_IMAGE_BASE + it }
    }

    /**
     * Get formatted file size (KB/MB)
     */
    fun getFormattedSize(): String {
        if (size == null) return "– KB"
        return when {
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    /**
     * Get dimensions as string
     */
    fun getDimensions(): String {
        return if (width != null && height != null) {
            "${width}×${height}"
        } else {
            "–"
        }
    }
}

/**
 * Image format variants from Strapi
 */
data class ImageFormats(
    @SerializedName("thumbnail") val thumbnail: ImageFormat?,
    @SerializedName("small") val small: ImageFormat?,
    @SerializedName("medium") val medium: ImageFormat?,
    @SerializedName("large") val large: ImageFormat?
)

data class ImageFormat(
    @SerializedName("url") val url: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("size") val size: Double?
)

/**
 * Response from /user-uploads/own/{id} DELETE
 */
data class DeleteUploadResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: DeletedUploadData?,
    @SerializedName("error") val error: String?
)

data class DeletedUploadData(
    @SerializedName("id") val id: Int?
)
