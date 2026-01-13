package de.meply.meply.data.profile

import com.google.gson.annotations.SerializedName

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
 * Request-Body für Updates:
 * Strapi erwartet { "data": { ... } }
 * Map mit Any? + @JvmSuppressWildcards, damit Retrofit/Gson problemlos serialisiert.
 */
data class UpdateProfileRequest(
    @SerializedName("data")
    @JvmSuppressWildcards
    val data: MutableMap<String, Any?>?
)
