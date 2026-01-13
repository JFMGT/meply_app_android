package de.meply.meply.data.follower

import com.google.gson.annotations.SerializedName

/**
 * User model for follower lists
 */
data class FollowerUser(
    @SerializedName("documentId") val documentId: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String?,
    @SerializedName("avatar") val avatar: List<AvatarUpload>?
)

data class AvatarUpload(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?
)

/**
 * Follow relationship model
 */
data class FollowRelation(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String,
    @SerializedName("follower") val follower: FollowerUser,
    @SerializedName("following") val following: FollowerUser,
    @SerializedName("status") val status: String, // "pending", "accepted", "declined"
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

/**
 * Response from follow list API
 */
data class FollowListResponse(
    @SerializedName("pending") val pending: MutableList<FollowRelation>,
    @SerializedName("followers") val followers: MutableList<FollowRelation>,
    @SerializedName("following") val following: MutableList<FollowRelation>,
    @SerializedName("blocked") val blocked: MutableList<FollowRelation>
)

/**
 * Request for managing follow relationships
 */
data class FollowManageRequest(
    @SerializedName("action") val action: String // "accepted", "declined", "remove"
)

/**
 * Response from follow management actions
 */
data class FollowManageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

/**
 * Request for toggling follow
 */
data class FollowToggleRequest(
    @SerializedName("documentId") val documentId: String
)

/**
 * Response from follow toggle
 */
data class FollowToggleResponse(
    @SerializedName("status") val status: String, // "followed" or "unfollowed"
    @SerializedName("message") val message: String?
)
