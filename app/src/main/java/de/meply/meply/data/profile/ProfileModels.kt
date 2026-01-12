package de.meply.meply.data.profile

/**
 * Entspricht den Profil-Attributen in Strapi.
 * Alle Felder nullable, weil Strapi je nach Nutzer unvollständig zurückliefern kann.
 */
data class ProfileAttributes(
    val username: String?,
    val birthDate: String?,                 // Format: "YYYY-MM-DD"
    val postalCode: String?,
    val city: String?,
    val searchRadius: Int?,
    val gender: String?,                    // "none" | "female" | "male" | "diverse" | "other"
    val boardgamegeekProfile: String?,
    val boardGameArenaUsername: String?,
    val showInUserList: Boolean?,
    val followPrivacy: String?,             // "open" | "request" (deprecated, use usersCanFollow)
    val usersCanFollow: String?,            // "open" | "request" (current field name)
    val allowProfileView: Boolean?,
    val showBoardGameRatings: Boolean?,

    // optional/technisch
    val latitude: Double?,
    val longitude: Double?,

    // In PHP wurde 'cords' als JSON-String abgelegt. Wenn ihr später echtes JSON wollt,
    // kann man das in Map<String, Double> ändern.
    val cords: String?
)

/**
 * Standard-Strapi-Wrapper für einen Eintrag:
 * { "data": { "id": "...", "attributes": { ... } } }
 */
data class ProfileItem(
    val id: String,
    val attributes: ProfileAttributes? // <- ohne `?`
)



/**
 * Generischer Single-Response-Wrapper, damit wir kompatibel zu Strapi bleiben.
 * Für PUT/GET von /profiles/... erwarten wir meistens genau dieses Format.
 */
data class ProfileResponse<T>(
    val data: T?
)

/**
 * Request-Body für Updates:
 * Strapi erwartet { "data": { ... } }
 * Map mit Any? + @JvmSuppressWildcards, damit Retrofit/Gson problemlos serialisiert.
 */
data class UpdateProfileRequest(
    @JvmSuppressWildcards
    val data: MutableMap<String, Any?>?
)
