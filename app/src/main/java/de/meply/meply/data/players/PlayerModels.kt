package de.meply.meply.data.players

import com.google.gson.annotations.SerializedName
import java.time.Instant

/**
 * A player match result from /matches/best endpoint
 */
data class PlayerMatch(
    @SerializedName("score") val score: Double?,
    @SerializedName("distance") val distance: Double?,
    @SerializedName("sharedGamesCount") val sharedGamesCount: Int?,
    @SerializedName("ranking") val ranking: Double?,
    @SerializedName("otherProfile") val otherProfile: MatchedProfile?,
    @SerializedName("availability") val availability: PlayerAvailability?
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

    /**
     * Check if this player is currently available to play
     */
    fun isReadyToPlay(): Boolean {
        return availability?.isActive() == true
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

/**
 * Availability data for a matched player
 */
data class PlayerAvailability(
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("hostingPreference") val hostingPreference: List<String>?,
    @SerializedName("note") val note: String?,
    @SerializedName("boardgames") val boardgames: List<AvailabilityBoardgame>?
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
 * Boardgame reference in availability
 */
data class AvailabilityBoardgame(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?
)

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
