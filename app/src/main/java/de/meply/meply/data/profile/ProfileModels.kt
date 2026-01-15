package de.meply.meply.data.profile

import com.google.gson.annotations.SerializedName

/**
 * Avatar upload data from Strapi
 */
data class AvatarUpload(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("formats") val formats: AvatarFormats?
)

data class AvatarFormats(
    @SerializedName("thumbnail") val thumbnail: AvatarFormat?
)

data class AvatarFormat(
    @SerializedName("url") val url: String?
)

/**
 * Entspricht den Profil-Attributen in Strapi.
 * Alle Felder nullable, weil Strapi je nach Nutzer unvollständig zurückliefern kann.
 */
data class ProfileAttributes(
    @SerializedName("username") val username: String?,
    @SerializedName("birthDate") val birthDate: String?,                 // Format: "YYYY-MM-DD"
    @SerializedName("postalCode") val postalCode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("searchRadius") val searchRadius: Int?,
    @SerializedName("gender") val gender: String?,                    // "none" | "female" | "male" | "diverse" | "other"
    @SerializedName("boardgamegeekProfile") val boardgamegeekProfile: String?,
    @SerializedName("boardGameArenaUsername") val boardGameArenaUsername: String?,
    @SerializedName("showInUserList") val showInUserList: Boolean?,
    @SerializedName("followPrivacy") val followPrivacy: String?,             // "open" | "request" (deprecated, use usersCanFollow)
    @SerializedName("usersCanFollow") val usersCanFollow: String?,            // "open" | "request" (current field name)
    @SerializedName("allowProfileView") val allowProfileView: Boolean?,
    @SerializedName("showBoardGameRatings") val showBoardGameRatings: Boolean?,

    // optional/technisch
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,

    // In PHP wurde 'cords' als JSON-String abgelegt. Wenn ihr später echtes JSON wollt,
    // kann man das in Map<String, Double> ändern.
    @SerializedName("cords") val cords: String?
)

/**
 * Standard-Strapi-Wrapper für einen Eintrag:
 * { "data": { "id": "...", "attributes": { ... } } }
 */
data class ProfileItem(
    @SerializedName("id") val id: String,
    @SerializedName("attributes") val attributes: ProfileAttributes?
)



/**
 * Generischer Single-Response-Wrapper, damit wir kompatibel zu Strapi bleiben.
 * Für PUT/GET von /profiles/... erwarten wir meistens genau dieses Format.
 */
data class ProfileResponse<T>(
    @SerializedName("data") val data: T?
)

/**
 * Special response for /profiles/me endpoint which returns a flat structure
 * All fields are directly in the data object (not nested in attributes)
 */
data class ProfileMeData(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("birthDate") val birthDate: String?,
    @SerializedName("postalCode") val postalCode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("searchRadius") val searchRadius: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("boardgamegeekProfile") val boardgamegeekProfile: String?,
    @SerializedName("boardGameArenaUsername") val boardGameArenaUsername: String?,
    @SerializedName("showInUserList") val showInUserList: Boolean?,
    @SerializedName("followPrivacy") val followPrivacy: String?,
    @SerializedName("usersCanFollow") val usersCanFollow: String?,
    @SerializedName("allowProfileView") val allowProfileView: Boolean?,
    @SerializedName("showBoardGameRatings") val showBoardGameRatings: Boolean?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("cords") val cords: Any?,  // Can be String or Object
    @SerializedName("avatar") val avatar: List<AvatarUpload>?,
    @SerializedName("scheduledDeletionAt") val scheduledDeletionAt: String? // Deletion date if account is marked for deletion
) {
    /**
     * Convert ProfileMeData to ProfileAttributes for use in ProfileItem
     */
    fun toProfileAttributes(): ProfileAttributes {
        return ProfileAttributes(
            username = username,
            birthDate = birthDate,
            postalCode = postalCode,
            city = city,
            searchRadius = searchRadius,
            gender = gender,
            boardgamegeekProfile = boardgamegeekProfile,
            boardGameArenaUsername = boardGameArenaUsername,
            showInUserList = showInUserList,
            followPrivacy = followPrivacy,
            usersCanFollow = usersCanFollow,
            allowProfileView = allowProfileView,
            showBoardGameRatings = showBoardGameRatings,
            latitude = latitude,
            longitude = longitude,
            cords = when (cords) {
                is String -> cords
                else -> null
            }
        )
    }

    /**
     * Convert ProfileMeData to ProfileItem
     */
    fun toProfileItem(): ProfileItem {
        return ProfileItem(
            id = id.toString(),
            attributes = toProfileAttributes()
        )
    }
}

/**
 * Request-Body für Updates:
 * Strapi erwartet { "data": { ... } }
 * Map mit Any? + @JvmSuppressWildcards, damit Retrofit/Gson problemlos serialisiert.
 */
data class UpdateProfileRequest(
    @SerializedName("data")
    @JvmSuppressWildcards
    val data: MutableMap<String, Any?>?
)
