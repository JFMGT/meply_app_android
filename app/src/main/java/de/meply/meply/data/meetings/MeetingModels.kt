package de.meply.meply.data.meetings // Oder Ihr entsprechendes Package

import com.google.gson.annotations.SerializedName

// Haupt-Datenklasse für ein Meeting
data class MeetingData(
    val id: Int,
    val documentId: String?,
    val title: String?,
    val description: String?,
    val date: String?, // Einfaches Datumsfeld, falls 'dates' nicht existiert
    val dates: MeetingDateInfo?, // Komplexes Datumsfeld
    val author: MeetingAuthor?,
    val location: MeetingLocationRelation?, // Verknüpfte Location
    val event: MeetingEventRelation?       // Verknüpftes Event
    // Ggf. weitere direkte Felder aus $m in PHP, die nicht Relationen sind
)

// Für die komplexe Datumsstruktur 'dates'
data class MeetingDateInfo(
    val type: String?, // "fixed", "range", "recurring", "eventDays"
    val value: MeetingDateValue?
)

data class MeetingDateValue(
    // Für type 'fixed'
    @SerializedName("date") // Explizit machen, falls JSON-Name "date" ist
    val fixedDateValue: String?, // Umbenannt, um Kollisionen zu vermeiden, falls "date" ein Feld in MeetingData ist

    // Für type 'range'
    @SerializedName("start") // Explizit machen
    val rangeStartValue: String?, // Umbenannt für Klarheit
    @SerializedName("end")   // Explizit machen
    val rangeEndValue: String?,   // Umbenannt für Klarheit

    // Für 'recurring' UND 'eventDays' - dieses Feld verarbeitet das JSON-Feld "days"
    @SerializedName("days")
    val daysList: List<Any>?, // Kann List<Int> ODER List<String> sein, Typ-Prüfung zur Laufzeit nötig

    // Nur relevant, wenn dates.type == "recurring"
    val frequency: String?,


    // --- ENTFERNT oder AUSKOMMENTIERT, da es den Konflikt verursacht hat ---
    // // Für type 'recurring'
    // val days: List<Int>?, // DIESES FELD HAT DEN KONFLIKT VERURSACHT
    // val frequency: String?,
    //
    // // Für type 'eventDays'
    // @SerializedName("days") val recurringOrEventDays: List<Any>? // Wurde jetzt zu daysList

    // --- ALTE STRUKTUR ENDE ---

    // Falls die JSON-Felder für 'fixed', 'range' *tatsächlich* 'date', 'start', 'end' heißen,
    // könnten Sie die ursprünglichen Namen beibehalten, solange sie innerhalb dieser Klasse eindeutig sind.
    // Die Umbenennung mit explizitem @SerializedName ist aber sicherer gegen Verwechslungen.
    // Beispiel, falls JSON-Felder 'date', 'start', 'end' sind:
    /*
    @SerializedName("date") val date: String?,
    @SerializedName("start") val start: String?,
    @SerializedName("end") val end: String?,

    @SerializedName("days")
    val daysList: List<Any>?,

    val frequency: String?,
    */
)

// Für den Autor des Meetings
data class MeetingAuthor(
    val id: Int?, // Nicht explizit im PHP-Zugriff, aber oft Teil von Relationen
    val username: String?,
    @SerializedName("userslug") val userSlug: String?, // PHP: $attributes['author']['userslug']
    val documentId: String?, // PHP: $attributes['author']['documentId']
    val city: String? // PHP: $attributes['author']['city']
    // Ggf. weitere Felder aus $attributes['author']
)

// Für die verknüpfte Location (basierend auf PHP-Zugriffen wie $attributes['location']['Ort'])
// Dies impliziert, dass 'location' ein Objekt ist, das diese Felder direkt enthält.
data class MeetingLocationRelation(
    @SerializedName("Ort") val ort: String?,
    @SerializedName("Titel") val titel: String?,
    val slug: String?,
    @SerializedName("Typ") val typ: String?
)

// Für das verknüpfte Event (basierend auf PHP-Zugriffen wie $attributes['event']['city'])
data class MeetingEventRelation(
    val city: String?,
    val slug: String?,
    @SerializedName("Title") val title: String?
)

// Wiederverwendung Ihrer StrapiListResponse
// data class StrapiListResponse<T>(val data: List<T>?, val meta: Meta?)
// Diese sollte bereits existieren und generisch sein.
