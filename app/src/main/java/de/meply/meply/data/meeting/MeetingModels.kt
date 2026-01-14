package de.meply.meply.data.meeting

import com.google.gson.annotations.SerializedName
import de.meply.meply.data.profile.AvatarUpload

// Author of a meeting
data class MeetingAuthor(
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("avatar") val avatar: List<AvatarUpload>?,
    @SerializedName("city") val city: String?
)

// Meeting location
data class MeetingLocation(
    @SerializedName("Ort") val ort: String?,
    @SerializedName("Titel") val titel: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("Typ") val typ: String?
)

// Meeting event
data class MeetingEvent(
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("Title") val title: String?
)

// Date information for meetings
data class MeetingDates(
    @SerializedName("type") val type: String?, // "fixed", "range", "recurring", "eventDays"
    @SerializedName("value") val value: Map<String, Any>? // Dynamic structure based on type
)

// Main Meeting model
data class Meeting(
    @SerializedName("id") val id: Int?,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("date") val date: String?, // Used for filtering
    @SerializedName("dates") val dates: MeetingDates?,
    @SerializedName("author") val author: MeetingAuthor?,
    @SerializedName("location") val location: MeetingLocation?,
    @SerializedName("event") val event: MeetingEvent?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

// API Response wrappers
data class MeetingsResponse(
    @SerializedName("data") val data: List<Meeting>?,
    @SerializedName("meta") val meta: MetaData?
)

data class MeetingResponse(
    @SerializedName("data") val data: Meeting?
)

data class MetaData(
    @SerializedName("pagination") val pagination: Pagination?
)

data class Pagination(
    @SerializedName("page") val page: Int?,
    @SerializedName("pageSize") val pageSize: Int?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("total") val total: Int?
)

// Request model for creating/updating meetings
data class CreateMeetingRequest(
    @SerializedName("data") val data: MeetingData
)

data class MeetingData(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("event") val event: String? = null,
    @SerializedName("dateType") val dateType: String? = null, // "fixed", "range", "recurring"
    @SerializedName("date") val date: String? = null, // For fixed date
    @SerializedName("dateFrom") val dateFrom: String? = null, // For range
    @SerializedName("dateTo") val dateTo: String? = null, // For range
    @SerializedName("recurringDays") val recurringDays: List<String>? = null, // For recurring
    @SerializedName("recurringFrequency") val recurringFrequency: String? = null, // For recurring
    @SerializedName("eventDays") val eventDays: List<String>? = null // For event meetings
)

// Delete request
data class DeleteMeetingRequest(
    @SerializedName("documentId") val documentId: String
)

data class DeleteMeetingResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?
)
