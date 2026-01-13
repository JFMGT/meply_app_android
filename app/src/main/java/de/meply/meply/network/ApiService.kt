package de.meply.meply.network

import com.google.gson.annotations.SerializedName
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
import de.meply.meply.data.feed.*
import de.meply.meply.data.messages.*
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part

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
        @SerializedName("id") val id: Int,
        @SerializedName("documentId") val documentId: String?,
        @SerializedName("username") val username: String,
        @SerializedName("email") val email: String,
        @SerializedName("profile") val profile: ProfileWrapper?
    )

    data class ProfileWrapper(
        @SerializedName("id") val id: String,
        @SerializedName("documentId") val documentId: String,
        @SerializedName("attributes") val attributes: de.meply.meply.data.profile.ProfileAttributes?
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




    @GET("users/me")
    fun getCurrentUser(
        @Query(value = "populate[profile][populate]", encoded = true) profilePopulate: String = "*"
    ): Call<UserMe>

    @GET("profiles/me")
    fun getMyProfile(): Call<ProfileResponse<de.meply.meply.data.profile.ProfileMeData>>

    @GET("profiles/{id}")
    fun getProfile(@Path("id") profileId: String): Call<ProfileResponse<ProfileItem>>

    @PUT("profiles/me")
    fun updateMyProfile(
        @Body update: UpdateProfileRequest
    ): Call<ProfileResponse<de.meply.meply.data.profile.ProfileMeData>>

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

    // ===== FEED ENDPOINTS =====

    /**
     * Get feed posts with pagination
     * @param limit Number of posts to fetch (1-50)
     * @param before Cursor for pagination (ISO 8601 timestamp)
     * @param since For newer posts
     * @param author Filter by author ID (optional)
     */
    @GET("feed")
    fun getFeed(
        @Query("limit") limit: Int = 10,
        @Query("before") before: String? = null,
        @Query("since") since: String? = null,
        @Query("author") author: String? = null
    ): Call<FeedResponse>

    /**
     * Create a new post
     */
    @POST("posts")
    fun createPost(
        @Body request: CreatePostRequest
    ): Call<CreatePostResponse>

    /**
     * Upload image for post
     * @param file Image file as MultipartBody.Part
     * @param alt Alternative text for accessibility
     * @param purpose Purpose of upload (e.g., "post", "avatar")
     * @param folder Optional folder path (e.g., "API Uploads")
     */
    @Multipart
    @POST("user-uploads/upload")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("alt") alt: RequestBody,
        @Part("purpose") purpose: RequestBody,
        @Part("folder") folder: RequestBody? = null
    ): Call<ImageUploadResponse>

    /**
     * Toggle like on a post
     */
    @POST("likes/toggle")
    fun toggleLike(
        @Body request: LikeToggleRequest
    ): Call<LikeToggleResponse>

    /**
     * Report a post
     */
    @POST("post-report")
    fun reportPost(
        @Body request: ReportPostRequest
    ): Call<ReportPostResponse>

    /**
     * Delete a post
     * @param documentId The document ID of the post to delete
     */
    @DELETE("post-delete/{documentId}")
    fun deletePost(
        @Path("documentId") documentId: String
    ): Call<Void>

    /**
     * Get post thread (post + all replies)
     * @param documentId The document ID of the post
     */
    @GET("post-tree/{documentId}")
    fun getPostThread(
        @Path("documentId") documentId: String
    ): Call<Post>

    // ===== PRIVATE MESSAGES ENDPOINTS =====

    /**
     * Get all conversations for the current user
     */
    @GET("conversations/me")
    fun getConversations(): Call<List<Conversation>>

    /**
     * Get messages in a specific conversation
     * @param conversationId The conversation document ID
     */
    @GET("conversations/{conversationId}/messages")
    fun getMessages(
        @Path("conversationId") conversationId: String
    ): Call<MessagesResponse>

    /**
     * Send a message in an existing conversation
     */
    @POST("messages")
    fun sendMessage(
        @Body request: SendMessageRequest
    ): Call<SendMessageResponse>

    /**
     * Create a new conversation with a message
     */
    @POST("conversations/create")
    fun createConversation(
        @Body request: CreateConversationRequest
    ): Call<SendMessageResponse>

    /**
     * Delete a conversation
     * @param conversationId The conversation document ID
     */
    @DELETE("conversations/{conversationId}")
    fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): Call<Void>

    // ===== INVITE CODES ENDPOINTS =====

    /**
     * Get or create invite codes for the current user
     */
    @POST("registration-codes/my-invite-codes")
    fun getMyInviteCodes(): Call<de.meply.meply.data.profile.InviteCodesResponse>

    // ===== USER PROFILE ENDPOINTS =====

    /**
     * Get user profile by username/slug
     * @param slug The username or slug of the user
     */
    @GET("profiles/slug/{slug}")
    fun getUserProfileBySlug(
        @Path("slug") slug: String
    ): Call<de.meply.meply.data.profile.UserProfileResponse>

    /**
     * Get matching score between two users
     * @param profileA The profile ID of user A
     * @param profileB The profile ID of user B
     */
    @GET("match/{profileA}/{profileB}")
    fun getMatchScore(
        @Path("profileA") profileA: String,
        @Path("profileB") profileB: String
    ): Call<de.meply.meply.data.profile.MatchScoreResponse>

    /**
     * Get shared highly rated board games between two users
     * @param profileA The profile ID of user A
     * @param profileB The profile ID of user B
     */
    @GET("boardgames/shared-highly-rated/{profileA}/{profileB}")
    fun getSharedHighlyRatedGames(
        @Path("profileA") profileA: String,
        @Path("profileB") profileB: String
    ): Call<List<de.meply.meply.data.profile.SharedGame>>

    /**
     * Check follow status between two users
     * @param userA The user document ID of user A
     * @param userB The user document ID of user B
     */
    @GET("followers/followedby/{userA}/{userB}")
    fun checkFollowStatus(
        @Path("userA") userA: String,
        @Path("userB") userB: String
    ): Call<de.meply.meply.data.profile.FollowStatusResponse>

    /**
     * Follow a user
     * @param request The follow request with userToFollow document ID
     */
    @POST("followers")
    fun followUser(
        @Body request: de.meply.meply.data.profile.FollowUserRequest
    ): Call<de.meply.meply.data.profile.FollowActionResponse>

    /**
     * Unfollow a user
     * @param request The unfollow request with userToUnfollow document ID
     */
    @POST("followers/unfollow")
    fun unfollowUser(
        @Body request: de.meply.meply.data.profile.UnfollowUserRequest
    ): Call<de.meply.meply.data.profile.FollowActionResponse>


}
