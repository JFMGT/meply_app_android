package de.meply.meply.data.uploads

import com.google.gson.annotations.SerializedName

/**
 * Response from /user-uploads/me
 * Lists all images uploaded by the current user
 */
data class UserUploadsResponse(
    @SerializedName("results") val results: List<UserUpload>?,
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
 * File details for an uploaded image
 */
data class UploadedFile(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @SerializedName("previewUrl") val previewUrl: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("ext") val ext: String?,
    @SerializedName("mime") val mime: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
) {
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

    /**
     * Get localized reason text
     */
    fun getLocalizedReason(reason: String?): String {
        return when (reason) {
            "post" -> "Beitrag"
            "avatar" -> "Profilbild"
            "message" -> "Nachricht"
            else -> reason ?: "Unbekannt"
        }
    }
}

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
