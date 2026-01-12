package de.meply.meply.network

import de.meply.meply.data.events.EventItem
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.data.events.StrapiSingleResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import de.meply.meply.data.AuthResponse
import de.meply.meply.data.LoginRequest
import retrofit2.http.Body
import retrofit2.http.POST
import de.meply.meply.data.events.FlatEventData
import de.meply.meply.data.meetings.MeetingData
import de.meply.meply.data.profile.ProfileItem
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.data.profile.UpdateProfileRequest
import retrofit2.http.PUT
import retrofit2.http.PATCH

interface ApiService {
    @POST("auth/local")
    fun login(
    @Body body: LoginRequest
    ): Call<AuthResponse>
    // Events Nearby (Liste)
    @GET("events/nearby")
    fun getNearbyEvents(
        @Query("zip") zip: String,
        @Query("radius") radius: Double,
        @Query("sort") sort: String = "start_date:asc",
        @Query("pagination[page]") page: Int = 1,
        @Query("pagination[pageSize]") pageSize: Int = 25
    ): Call<StrapiListResponse<EventItem>>

    // Event per Slug suchen – $ in [$eq] escapen!
    @GET("events/")
    fun getEventBySlug(
        @Query(value = "filters[documentId][\$eq]", encoded = true) slug: String,
        @Query(value = "populate[location][populate]", encoded = true) popLoc: String = "*",
        @Query(value = "populate[organizer][populate]", encoded = true) popOrg: String = "*",
        //@Query(value = "pagination[pageSize]", encoded = true) pageSize: Int = 1
    ): Call<StrapiListResponse<FlatEventData>>

    // User-Model
    data class UserMe(
        val id: Int,
        val username: String,
        val email: String,
        val profile: ProfileWrapper?
    )

    data class ProfileWrapper(
        val id: String,
        val documentId: String,
        val attributes: de.meply.meply.data.profile.ProfileAttributes?
    )
    data class ProfileData(
        val id: Int,
        val documentId: String,
        val username: String?,
        val birthDate: String?,                 // neu
        val postalCode: String?,
        val city: String?,
        val searchRadius: Int?,
        val gender: String?,                    // neu
        val boardgamegeekProfile: String?,
        val boardGameArenaUsername: String?,    // neu
        val showInUserList: Boolean?,           // neu
        val followPrivacy: String?,             // neu
        val allowProfileView: Boolean?,         // neu
        val showBoardGameRatings: Boolean?,     // neu
        val latitude: Double?,                  // neu
        val longitude: Double?,                 // neu
        val cords: String?
    )




    @GET("users/me?populate=profile")
    fun getCurrentUser(): Call<UserMe>

    @GET("profiles/{id}")
    fun getProfile(@Path("id") profileId: String): Call<ProfileResponse<ProfileItem>>

    @PUT("profiles/{id}")
    fun updateProfile(
        @Path("id") profileId: String,
        @Body update: UpdateProfileRequest
    ): Call<ProfileResponse<ProfileItem>>



    // Detail per Slug – exakt wie im Web (keine pagination)
    @GET("events/") // trailing slash wie im Web
    fun getEventBySlugRaw(
        @QueryMap(encoded = true) query: Map<String, String>
    ): Call<StrapiListResponse<EventItem>>

    // Detail per documentId – ebenfalls ohne pagination
    @GET("events/")
    fun getEventByDocumentIdRaw(
        @QueryMap(encoded = true) query: Map<String, String>
    ): Call<StrapiListResponse<EventItem>>

    // Detail per numerischer ID
    @GET("events/{id}")
    fun getEventById(
        @Path("id") id: String,
        @Query("populate[location][populate]") popLoc: String = "*",
        @Query("populate[organizer][populate]") popOrg: String = "*"
    ): Call<StrapiSingleResponse<EventItem>>

    @GET("meetings")
    fun getMeetingsForEvent(
        // @Header("Authorization") authToken: String, // << ENTFERNT, da ApiClient das managed
        @Query("filters[event][documentId][\$eq]") eventDocumentId: String,
        @Query("sort") sortBy: String = "date:asc",
        @Query("populate") populateFields: String = "*",
        @Query("filters[\$and][0][\$or][0][date][\$null]") dateIsNull: Boolean = true,
        @Query("filters[\$and][0][\$or][1][date][\$gte]") dateIsGte: String // Hier kommt das heutige Datum rein
    ): Call<StrapiListResponse<MeetingData>>


}
