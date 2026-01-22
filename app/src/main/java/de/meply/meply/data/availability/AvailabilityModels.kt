package de.meply.meply.data.availability

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Data models for User Availability ("Spielbereit") feature
 */

/**
 * Boardgame reference in availability response
 */
data class AvailabilityBoardgame(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?
)

/**
 * User availability data returned from API
 */
data class UserAvailability(
    @SerializedName("id") val id: String?,
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("hostingPreference") val hostingPreference: List<String>?,
    @SerializedName("note") val note: String?,
    @SerializedName("boardgames") val boardgames: List<AvailabilityBoardgame>?,
    @SerializedName("isActive") val isActive: Boolean?
) {
    /**
     * Check if the availability is still active (not expired)
     */
    fun isCurrentlyActive(): Boolean {
        if (isActive == false) return false
        if (expiresAt.isNullOrEmpty()) return false

        return try {
            val expiry = Instant.parse(expiresAt)
            expiry.isAfter(Instant.now())
        } catch (e: DateTimeParseException) {
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
 * Response wrapper for GET /user-availabilities/my
 */
data class UserAvailabilityResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UserAvailability?,
    @SerializedName("message") val message: String?
)

/**
 * Request body for POST /user-availabilities/my
 */
data class SetAvailabilityRequest(
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("hostingPreference") val hostingPreference: List<String>,
    @SerializedName("boardgames") val boardgames: List<Int>? = null,
    @SerializedName("note") val note: String? = null
)

/**
 * Response wrapper for POST/DELETE /user-availabilities/my
 */
data class AvailabilityActionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserAvailability?
)
