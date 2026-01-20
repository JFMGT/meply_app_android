package de.meply.meply.data.events

import com.google.gson.annotations.SerializedName

/**
 * Response from /events endpoint (Strapi format)
 */
data class EventsResponse(
    @SerializedName("data") val data: List<Event>?,
    @SerializedName("meta") val meta: EventsMeta?
)

data class EventsMeta(
    @SerializedName("pagination") val pagination: EventsPagination?
)

data class EventsPagination(
    @SerializedName("page") val page: Int?,
    @SerializedName("pageSize") val pageSize: Int?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("total") val total: Int?
)

/**
 * Single response from /events/{id}
 */
data class EventSingleResponse(
    @SerializedName("data") val data: Event?
)

/**
 * A single event entry
 */
data class Event(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("Title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("fixed_date") val fixedDate: Boolean?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("date_placeholder") val datePlaceholder: String?,
    @SerializedName("repeat") val repeat: String?,
    @SerializedName("street") val street: String?,
    @SerializedName("street_number") val streetNumber: String?,
    @SerializedName("zip") val zip: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("coordinates") val coordinates: EventCoordinates?,
    @SerializedName("location") val location: EventLocation?,
    @SerializedName("author") val author: EventAuthor?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
) {
    /**
     * Get formatted address
     */
    fun getFormattedAddress(): String {
        val parts = mutableListOf<String>()

        if (!street.isNullOrBlank()) {
            val streetPart = if (!streetNumber.isNullOrBlank()) "$street $streetNumber" else street
            parts.add(streetPart)
        }

        if (!zip.isNullOrBlank() || !city.isNullOrBlank()) {
            val cityPart = listOfNotNull(zip, city).joinToString(" ")
            parts.add(cityPart)
        }

        return parts.joinToString(", ").ifEmpty { "–" }
    }

    /**
     * Get formatted date range
     */
    fun getFormattedDateRange(): String {
        if (!datePlaceholder.isNullOrBlank()) {
            return datePlaceholder
        }

        val start = formatDateForDisplay(startDate)
        val end = formatDateForDisplay(endDate)

        return when {
            start.isNotEmpty() && end.isNotEmpty() && start != end -> "$start - $end"
            start.isNotEmpty() -> start
            else -> "Kein Datum"
        }
    }

    /**
     * Get formatted time range
     */
    fun getFormattedTimeRange(): String {
        val start = startTime?.take(5) ?: ""
        val end = endTime?.take(5) ?: ""

        return when {
            start.isNotEmpty() && end.isNotEmpty() -> "$start - $end Uhr"
            start.isNotEmpty() -> "ab $start Uhr"
            else -> ""
        }
    }

    private fun formatDateForDisplay(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Get repeat text
     */
    fun getRepeatText(): String {
        return when (repeat) {
            "weekly" -> "Wöchentlich"
            "monthly" -> "Monatlich"
            "nth_weekday" -> "Wochentag im Monat"
            else -> ""
        }
    }

    /**
     * Check if this event is published
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

data class EventCoordinates(
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?
)

data class EventLocation(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("Titel") val titel: String?
)

data class EventAuthor(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("username") val username: String?
)

/**
 * Request to create/update an event
 */
data class CreateEventRequest(
    @SerializedName("data") val data: EventData
)

data class EventData(
    @SerializedName("Title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("fixed_date") val fixedDate: Boolean?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("date_placeholder") val datePlaceholder: String?,
    @SerializedName("repeat") val repeat: String?,
    @SerializedName("street") val street: String?,
    @SerializedName("street_number") val streetNumber: String?,
    @SerializedName("zip") val zip: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("location") val locationId: Int?,
    @SerializedName("coordinates") val coordinates: EventCoordinates?
)

/**
 * Response from create/update event
 */
data class EventActionResponse(
    @SerializedName("data") val data: Event?
)

/**
 * Event repeat options
 */
enum class EventRepeat(val value: String, val displayName: String) {
    NONE("none", "Keine Wiederholung"),
    WEEKLY("weekly", "Wöchentlich"),
    MONTHLY("monthly", "Monatlich"),
    NTH_WEEKDAY("nth_weekday", "Wochentag im Monat");

    companion object {
        fun fromString(value: String?): EventRepeat {
            return entries.find { it.value == value } ?: NONE
        }

        fun getAllOptions(): List<Pair<String, String>> {
            return entries.map { it.value to it.displayName }
        }
    }
}
