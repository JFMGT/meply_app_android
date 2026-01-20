package de.meply.meply.data.locations

import com.google.gson.annotations.SerializedName

/**
 * Response from /locations endpoint (Strapi format)
 */
data class LocationsResponse(
    @SerializedName("data") val data: List<Location>?,
    @SerializedName("meta") val meta: LocationsMeta?
)

data class LocationsMeta(
    @SerializedName("pagination") val pagination: LocationsPagination?
)

data class LocationsPagination(
    @SerializedName("page") val page: Int?,
    @SerializedName("pageSize") val pageSize: Int?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("total") val total: Int?
)

/**
 * Single response from /locations/{id}
 */
data class LocationSingleResponse(
    @SerializedName("data") val data: Location?
)

/**
 * A single location entry
 */
data class Location(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("Titel") val titel: String?,
    @SerializedName("Strasse") val strasse: String?,
    @SerializedName("Hausnummer") val hausnummer: String?,
    @SerializedName("PLZ") val plz: String?,
    @SerializedName("Ort") val ort: String?,
    @SerializedName("Typ") val typ: String?,
    @SerializedName("Beschreibung") val beschreibung: String?,
    @SerializedName("Mail") val mail: String?,
    @SerializedName("Website") val website: String?,
    @SerializedName("Telefon") val telefon: String?,
    @SerializedName("allow_user_events") val allowUserEvents: Boolean?,
    @SerializedName("coordinates") val coordinates: LocationCoordinates?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("author") val author: LocationAuthor?
) {
    /**
     * Get formatted address
     */
    fun getFormattedAddress(): String {
        val parts = mutableListOf<String>()

        if (!strasse.isNullOrBlank()) {
            val street = if (!hausnummer.isNullOrBlank()) "$strasse $hausnummer" else strasse
            parts.add(street)
        }

        if (!plz.isNullOrBlank() || !ort.isNullOrBlank()) {
            val cityPart = listOfNotNull(plz, ort).joinToString(" ")
            parts.add(cityPart)
        }

        return parts.joinToString(", ").ifEmpty { "–" }
    }

    /**
     * Get localized type name
     */
    fun getLocalizedType(): String {
        return typ ?: "Unbekannt"
    }

    /**
     * Get icon name for the type
     */
    fun getTypeIcon(): String {
        return when (typ) {
            "Geschäft" -> "store"
            "Cafe" -> "cafe"
            "Club" -> "group"
            "Location" -> "location"
            else -> "location"
        }
    }

    /**
     * Check if this location is published
     */
    fun isPublished(): Boolean {
        return !publishedAt.isNullOrBlank()
    }

    /**
     * Get status text
     */
    fun getStatusText(): String {
        return if (isPublished()) "Veröffentlicht" else "Entwurf"
    }
}

data class LocationCoordinates(
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?
)

data class LocationAuthor(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("username") val username: String?
)

/**
 * Request to create/update a location
 */
data class CreateLocationRequest(
    @SerializedName("data") val data: LocationData
)

data class LocationData(
    @SerializedName("Titel") val titel: String,
    @SerializedName("Strasse") val strasse: String?,
    @SerializedName("Hausnummer") val hausnummer: String?,
    @SerializedName("PLZ") val plz: String?,
    @SerializedName("Ort") val ort: String?,
    @SerializedName("Typ") val typ: String,
    @SerializedName("Beschreibung") val beschreibung: String?,
    @SerializedName("Mail") val mail: String?,
    @SerializedName("Website") val website: String?,
    @SerializedName("Telefon") val telefon: String?,
    @SerializedName("allow_user_events") val allowUserEvents: Boolean?,
    @SerializedName("coordinates") val coordinates: LocationCoordinates?
)

/**
 * Response from create/update location
 */
data class LocationActionResponse(
    @SerializedName("data") val data: Location?
)

/**
 * Location type enum
 */
enum class LocationType(val displayName: String) {
    GESCHAEFT("Geschäft"),
    CAFE("Cafe"),
    CLUB("Club"),
    LOCATION("Location");

    companion object {
        fun fromString(value: String?): LocationType? {
            return entries.find { it.displayName == value }
        }

        fun getAllTypes(): List<String> {
            return entries.map { it.displayName }
        }
    }
}
