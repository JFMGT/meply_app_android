package de.meply.meply.data.events

import com.google.gson.annotations.SerializedName

// ---------- Strapi-Response-Hüllen ----------
data class StrapiListResponse<T>(
    val data: List<T> = emptyList(),
    val meta: Meta? = null // <--- HINZUGEFÜGT: das meta-Feld
)

data class Meta(
    val pagination: Pagination?
    // Fügen Sie hier weitere Felder hinzu, falls Ihr 'meta'-Objekt von Strapi mehr enthält
)

data class StrapiSingleResponse<T>(val data: T? = null)

// ---------- Event-Objekte ----------
data class EventItem(
    val id: Int,
    val attributes: EventAttributes
)

data class Pagination(
    val page: Int?,
    val pageSize: Int?,
    val pageCount: Int?,
    val total: Int?
)

data class FlatEventData(
    val id: Int,
    @SerializedName("Title") val title: String?,
    val slug: String?,
    val description: String?,
    val city: String?,
    val street: String?,
    @SerializedName("street_number") val streetNumber: String?,
    val zip: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("fixed_date") val fixedDate: Boolean?,
    @SerializedName("date_placeholder") val datePlaceholder: String?,
    val url: String?,
    @SerializedName("documentId") val documentId: String?,
    // Relationen (müssen auch angepasst werden, wenn sie flach sind oder anders heißen)
    // Wenn location und organizer auch flach in der Antwort sind oder
    // ihre Struktur bekannt ist, muss das hier abgebildet werden.
    // Fürs Erste lasse ich sie weg, da sie in Ihrem Log nicht explizit sichtbar waren,
    // außer dass populate verwendet wurde. Wie sehen die aus?
    // val location: LocationData?, // Beispiel
    // val organizer: OrganizerData?, // Beispiel
    @SerializedName("meetingCount") val meetingCount: Int?, // Falls vorhanden

    // Die Felder aus der geloggten Antwort
    val repeat: String?, // War null, aber Typ muss definiert werden
    val createdAt: String?,
    val updatedAt: String?,
    val publishedAt: String?,
    val country: String?,
    val likes: Int?,
    // coordinates war ein Objekt, das müsste auch eine eigene Datenklasse sein
    val coordinates: CoordinatesData?
    // val authorCanEdit: Any? // Typ unklar, Any? oder spezifischer Typ
)

data class CoordinatesData(
    val lat: Double?,
    val lng: Double?
)

data class EventAttributes(
    // Basis
    @SerializedName("Title") val title: String?,
    val slug: String?,
    val description: String?,

    // Ort/Adresse
    val city: String?,
    @SerializedName("street")        val street: String?,
    @SerializedName("street_number") val streetNumber: String?,
    val zip: String?,

    // Zeiten/Datum
    @SerializedName("start_date") val startDate: String?,   // "yyyy-MM-dd"
    @SerializedName("end_date")   val endDate: String?,     // "yyyy-MM-dd"
    @SerializedName("start_time") val startTime: String?,   // "HH:mm:ss" (ggf. anpassen an dein Backend)
    @SerializedName("end_time")   val endTime: String?,     // "HH:mm:ss"
    @SerializedName("fixed_date") val fixedDate: Boolean?,
    @SerializedName("date_placeholder") val datePlaceholder: String?,

    // Links/IDs
    val url: String?,
    @SerializedName("documentId") val documentId: String?,

    // Relationen (per populate geladen)
    val location: RelationOne<LocationData>?,
    val organizer: RelationOne<OrganizerData>?,

    // Sonstiges
    @SerializedName("meetingCount") val meetingCount: Int?
)

// ---------- Generische One-to-One-Relation (Strapi) ----------
data class RelationOne<T>(val data: RelData<T>?)
data class RelData<T>(val id: Int?, val attributes: T?)

// ---------- Attribute der Relationen ----------
data class LocationData(
    // In deinem PHP heißt das Feld "Titel"
    @SerializedName("Titel") val title: String?
)

data class OrganizerData(
    @SerializedName("Titel") val title: String?
)
