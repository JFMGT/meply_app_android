package de.meply.meply.data.players

import com.google.gson.annotations.SerializedName

/**
 * A player match result from /matches/best endpoint
 */
data class PlayerMatch(
    @SerializedName("score") val score: Double?,
    @SerializedName("distance") val distance: Double?,
    @SerializedName("sharedGamesCount") val sharedGamesCount: Int?,
    @SerializedName("otherProfile") val otherProfile: MatchedProfile?
) {
    /**
     * Get score as percentage (0-100)
     */
    fun getScorePercent(): Int {
        return ((score ?: 0.0) * 100).toInt()
    }

    /**
     * Get formatted distance string
     */
    fun getFormattedDistance(): String {
        val dist = distance ?: return "–"
        return if (dist < 1) {
            "< 1 km"
        } else {
            "${dist.toInt()} km"
        }
    }
}

/**
 * Profile data for a matched player
 */
data class MatchedProfile(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("avatar") val avatar: List<AvatarData>?
) {
    /**
     * Get avatar URL or null
     */
    fun getAvatarUrl(): String? {
        return avatar?.firstOrNull()?.url
    }
}

data class AvatarData(
    @SerializedName("url") val url: String?,
    @SerializedName("formats") val formats: AvatarFormats?
)

data class AvatarFormats(
    @SerializedName("thumbnail") val thumbnail: AvatarFormat?,
    @SerializedName("small") val small: AvatarFormat?
)

data class AvatarFormat(
    @SerializedName("url") val url: String?
)
