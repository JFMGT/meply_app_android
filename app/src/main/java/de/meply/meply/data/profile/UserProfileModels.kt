package de.meply.meply.data.profile

import com.google.gson.annotations.SerializedName
import java.time.Instant

/**
 * Response from GET /profiles/slug/{username}
 * Returns a flat profile structure with avatar
 */
data class UserProfileResponse(
    @SerializedName("data") val data: UserProfileData?
)

/**
 * User profile data with all fields and avatar
 */
data class UserProfileData(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("userDocumentId") val userDocumentId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("birthDate") val birthDate: String?,
    @SerializedName("postalCode") val postalCode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("searchRadius") val searchRadius: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("boardgamegeekProfile") val boardgamegeekProfile: String?,
    @SerializedName("boardGameArenaProfile") val boardGameArenaProfile: String?,
    @SerializedName("boardGameArenaUsername") val boardGameArenaUsername: String?,
    @SerializedName("allowProfileView") val allowProfileView: Boolean?,
    @SerializedName("showBoardGameRatings") val showBoardGameRatings: Boolean?,
    @SerializedName("avatar") val avatar: List<AvatarImage>?,
    @SerializedName("availability") val availability: ProfileAvailability?
)

data class AvatarImage(
    @SerializedName("url") val url: String
)

/**
 * Availability data for a user profile
 */
data class ProfileAvailability(
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("hostingPreference") val hostingPreference: List<String>?,
    @SerializedName("note") val note: String?,
    @SerializedName("boardgames") val boardgames: List<ProfileAvailabilityGame>?
) {
    /**
     * Check if the availability is still active (not expired)
     */
    fun isActive(): Boolean {
        if (expiresAt.isNullOrEmpty()) return false
        return try {
            val expiry = Instant.parse(expiresAt)
            expiry.isAfter(Instant.now())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get hosting preference as display string
     */
    fun getHostingDisplayText(): String {
        val prefs = hostingPreference ?: return ""
        val texts = mutableListOf<String>()
        if (prefs.contains("home")) texts.add("Gastgeber")
        if (prefs.contains("away")) texts.add("Gast")
        if (prefs.contains("neutral")) texts.add("Neutral")
        return texts.joinToString(" / ")
    }
}

/**
 * Boardgame reference in profile availability
 */
data class ProfileAvailabilityGame(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?
)

/**
 * Response from GET /match/{profileA}/{profileB}
 * Returns matching score and distance between two users
 */
data class MatchScoreResponse(
    @SerializedName("score") val score: Double?,
    @SerializedName("sharedCount") val sharedCount: Int?,
    @SerializedName("distance") val distance: Double?
)

/**
 * Response from GET /boardgames/shared-highly-rated/{profileA}/{profileB}
 * Returns list of shared highly rated board games
 */
data class SharedGame(
    @SerializedName("boardgame") val boardgame: BoardGameInfo,
    @SerializedName("ratingA") val ratingA: Int?,
    @SerializedName("ratingB") val ratingB: Int?
)

data class BoardGameInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?
)

/**
 * Response from GET /followers/followedby/{userA}/{userB}
 * Returns follow status between two users
 */
data class FollowStatusResponse(
    @SerializedName("isFollowing") val isFollowing: Boolean?
)

/**
 * Request for POST /followers (follow a user)
 */
data class FollowUserRequest(
    @SerializedName("userToFollow") val userToFollow: String
)

/**
 * Request for DELETE /followers/{documentId} (unfollow a user)
 */
data class UnfollowUserRequest(
    @SerializedName("userToUnfollow") val userToUnfollow: String
)

/**
 * Response from follow/unfollow operations
 */
data class FollowActionResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?
)
