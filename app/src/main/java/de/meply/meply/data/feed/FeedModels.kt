package de.meply.meply.data.feed

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/feed
 */
data class FeedResponse(
    @SerializedName("feed") val feed: List<Post>,
    @SerializedName("cursor") val cursor: FeedCursor?,
    @SerializedName("hasMore") val hasMore: Boolean = false,
    @SerializedName("nearby") val nearby: NearbyInfo? = null
)

/**
 * Nearby info in feed response (when nearby=true)
 */
data class NearbyInfo(
    @SerializedName("radius") val radius: Int?
)

data class FeedCursor(
    @SerializedName("oldestCreatedAt") val oldestCreatedAt: String?,
    @SerializedName("newestCreatedAt") val newestCreatedAt: String?,
    @SerializedName("hasMore") val hasMore: Boolean = false
)

/**
 * Post data structure
 */
data class Post(
    @SerializedName("documentId") val documentId: String,
    @SerializedName("id") val id: Int = 0,
    @SerializedName("content") val content: String?,
    @SerializedName("visibility") val visibility: String = "members", // public, members, follower
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("deletedAt") val deletedAt: String? = null, // Timestamp when post was deleted (soft delete)
    @SerializedName(value = "liked", alternate = ["isLiked"]) val liked: Boolean = false,
    @SerializedName(value = "likeCount", alternate = ["likes"]) val likeCount: Int = 0,
    @SerializedName("replyCount") val replyCount: Int = 0,
    @SerializedName("author") val author: PostAuthor? = null,
    @SerializedName("image") val image: List<PostImage>? = null,
    @SerializedName("parent") val parent: PostParent? = null,
    @SerializedName("children") val children: List<Post>? = null // For thread view
) {
    /**
     * Check if this post has been deleted (soft delete)
     * A post is considered deleted if:
     * - deletedAt is not null, OR
     * - author is null (legacy indicator for deleted posts)
     */
    val isDeleted: Boolean
        get() = deletedAt != null || author == null
}

data class PostAuthor(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("username") val username: String,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("avatar") val avatar: List<AvatarImage>? = null
)

data class AvatarImage(
    @SerializedName("formats") val formats: ImageFormats?
)

data class ImageFormats(
    @SerializedName("thumbnail") val thumbnail: ImageFormat?,
    @SerializedName("medium") val medium: ImageFormat?,
    @SerializedName("small") val small: ImageFormat?
)

data class ImageFormat(
    @SerializedName("url") val url: String
)

data class PostImage(
    @SerializedName("url") val url: String,
    @SerializedName("alternativeText") val alternativeText: String?,
    @SerializedName("formats") val formats: ImageFormats?
)

data class PostParent(
    @SerializedName("documentId") val documentId: String
)

/**
 * Request for POST /api/posts
 */
data class CreatePostRequest(
    @SerializedName("data") val data: CreatePostData
)

data class CreatePostData(
    @SerializedName("content") val content: String,
    @SerializedName("image") val image: List<Int> = emptyList(),
    @SerializedName("visibility") val visibility: String = "members",
    @SerializedName("parent") val parent: PostParentConnect? = null
)

data class PostParentConnect(
    @SerializedName("connect") val connect: PostParentDocumentId
)

data class PostParentDocumentId(
    @SerializedName("documentId") val documentId: String
)

/**
 * Response from POST /api/posts
 */
data class CreatePostResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("response") val response: Any?,
    @SerializedName("raw") val raw: String?
)

/**
 * Request for POST /api/likes/toggle
 */
data class LikeToggleRequest(
    @SerializedName("targetDocumentId") val targetDocumentId: String,
    @SerializedName("targetType") val targetType: String = "post"
)

/**
 * Response from POST /api/likes/toggle
 */
data class LikeToggleResponse(
    @SerializedName("status") val status: String?, // "liked" or "unliked"
    @SerializedName("likeCount") val likeCount: Int?,
    @SerializedName("likesCount") val likesCount: Int?, // Alternative field name
    @SerializedName("likes_count") val likes_count: Int?, // Another alternative
    @SerializedName("count") val count: Int? // Another alternative
) {
    // Check if any like count field is present in the response
    fun hasLikeCount(): Boolean = likeCount != null || likesCount != null || likes_count != null || count != null

    // Get the actual like count from whichever field is present
    fun getActualLikeCount(): Int = likeCount ?: likesCount ?: likes_count ?: count ?: 0
}

/**
 * Response from GET /api/likes/has-liked
 * Returns list of document IDs that the user has liked
 */
data class HasLikedResponse(
    @SerializedName("liked") val liked: List<String>? = null
)

/**
 * Request for POST /api/post-report
 */
data class ReportPostRequest(
    @SerializedName("targetDocumentId") val targetDocumentId: String,
    @SerializedName("targetType") val targetType: String = "post",
    @SerializedName("reason") val reason: String
)

/**
 * Response from POST /api/post-report
 */
data class ReportPostResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("status") val status: String?,
    @SerializedName("reportId") val reportId: String?,
    @SerializedName("error") val error: String?
)

/**
 * Response from image upload
 */
data class ImageUploadResponse(
    @SerializedName("data") val data: ImageUploadData?,
    @SerializedName("uploads") val uploads: List<ImageUploadItem>?
)

data class ImageUploadData(
    @SerializedName("file") val file: Int
)

data class ImageUploadItem(
    @SerializedName("uploadId") val uploadId: Int
)

/**
 * Standard Strapi upload response (from /upload endpoint)
 */
data class StrapiUploadResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?
)

/**
 * Generic error response
 */
data class ErrorResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String?
)
