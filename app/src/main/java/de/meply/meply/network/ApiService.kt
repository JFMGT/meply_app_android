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
import okhttp3.RequestBody
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

    @GET("profiles/me?populate=avatar")
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

    /**
     * Get meetings by author (for user profile view)
     * @param authorDocumentId The author's profile document ID
     * @param dateIsGte Filter for future meetings
     */
    @GET("meetings")
    fun getMeetingsByAuthor(
        @Query("filters[author][documentId][\$eq]") authorDocumentId: String,
        @Query("sort") sortBy: String = "date:asc",
        @Query("populate") populateFields: String = "*",
        @Query("filters[\$and][0][\$or][0][date][\$null]") dateIsNull: Boolean = true,
        @Query("filters[\$and][0][\$or][1][date][\$gte]") dateIsGte: String
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
     * Simple Strapi file upload (used for avatars)
     * Uses the standard Strapi upload endpoint
     * @param file Image file as MultipartBody.Part
     */
    @Multipart
    @POST("upload")
    fun uploadFile(
        @Part file: MultipartBody.Part
    ): Call<List<StrapiUploadResponse>>

    /**
     * Delete uploaded file from Strapi
     * @param fileId The ID of the file to delete
     */
    @DELETE("upload/files/{id}")
    fun deleteUploadedFile(
        @Path("id") fileId: Int
    ): Call<StrapiUploadResponse>

    /**
     * Toggle like on a post
     */
    @POST("likes/toggle")
    fun toggleLike(
        @Body request: LikeToggleRequest
    ): Call<LikeToggleResponse>

    /**
     * Check which documents the user has liked
     * @param documentIds List of document IDs to check
     * @return HasLikedResponse with list of liked document IDs
     */
    @GET("likes/has-liked")
    fun hasLiked(
        @Query("documentIds[]") documentIds: List<String>
    ): Call<HasLikedResponse>

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
     * @param conversationId The conversation ID (integer)
     */
    @DELETE("conversations/{conversationId}")
    fun deleteConversation(
        @Path("conversationId") conversationId: Int
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
     * Get user's flea market sales listings
     * @param profileId The profile ID of the user
     */
    @GET("user-boardgames/sales/{profileId}")
    fun getUserSales(
        @Path("profileId") profileId: String
    ): Call<de.meply.meply.data.profile.UserSalesResponse>

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

    // ===== FOLLOWER MANAGEMENT ENDPOINTS =====

    /**
     * Get followers by status
     * @param userA The user document ID or "all"
     * @param userB The user document ID or "all"
     * @param status The status filter: "pending", "accepted", "declined"
     */
    @GET("followers/followedby/{userA}/{userB}")
    fun getFollowersByStatus(
        @Path("userA") userA: String,
        @Path("userB") userB: String,
        @Query("status") status: String
    ): Call<de.meply.meply.data.follower.FollowedByResponse>

    /**
     * Manage a follow request (accept, decline, remove)
     * @param followId The document ID of the follow relationship
     * @param request The action to perform (accepted, declined, remove)
     */
    @PATCH("followers/manage/{followId}")
    fun manageFollow(
        @Path("followId") followId: String,
        @Body request: de.meply.meply.data.follower.FollowManageRequest
    ): Call<de.meply.meply.data.follower.FollowManageResponse>

    /**
     * Toggle follow status (follow/unfollow)
     * @param request The user document ID to toggle follow
     */
    @POST("followers/toggle")
    fun toggleFollow(
        @Body request: de.meply.meply.data.follower.FollowToggleRequest
    ): Call<de.meply.meply.data.follower.FollowToggleResponse>

    // ===== MEETING/GESUCHE ENDPOINTS =====

    /**
     * Get meetings for the current user
     * @param authorDocumentId The author's user document ID
     * @param dateFilter Date to filter meetings (use "1984-05-05" to get all)
     */
    @GET("meetings")
    fun getUserMeetings(
        @Query("filters[author][documentId][\$eq]") authorDocumentId: String,
        @Query("filters[\$and][0][\$or][0][date][\$null]") dateIsNull: Boolean = true,
        @Query("filters[\$and][0][\$or][1][date][\$gte]") dateIsGte: String = "1984-05-05",
        @Query("populate") populate: String = "*",
        @Query("sort") sort: String = "date:asc"
    ): Call<de.meply.meply.data.meeting.MeetingsResponse>

    /**
     * Create a new meeting
     * @param request The meeting data
     */
    @POST("meetings")
    fun createMeeting(
        @Body request: de.meply.meply.data.meeting.CreateMeetingRequest
    ): Call<de.meply.meply.data.meeting.MeetingResponse>

    /**
     * Delete a meeting
     * @param documentId The meeting document ID
     */
    @DELETE("meetings/{documentId}")
    fun deleteMeeting(
        @Path("documentId") documentId: String
    ): Call<Void>

    // ===== MY COLLECTION / BOARDGAMES ENDPOINTS =====

    /**
     * Get user's boardgame collection
     * @param page Page number (1-based)
     * @param pageSize Items per page
     * @param state Filter by state (wishlist, played, owned)
     * @param minRating Filter by minimum rating
     * @param title Filter by title (contains)
     * @param forSale Filter by sale status
     * @param sortBy Sort by field (title, rating)
     */
    @GET("user-boardgames/my-collection")
    fun getMyCollection(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("state") state: String? = null,
        @Query("minRating") minRating: Float? = null,
        @Query("title") title: String? = null,
        @Query("forSale") forSale: String? = null,
        @Query("sortBy") sortBy: String = "title"
    ): Call<de.meply.meply.data.collection.MyCollectionResponse>

    /**
     * Search boardgames by title
     * @param query Search query (min 2 characters)
     */
    @GET("boardgames")
    fun searchBoardgames(
        @Query("filters[title][\$containsi]") query: String,
        @Query("pagination[limit]") limit: Int = 10
    ): Call<de.meply.meply.data.events.StrapiListResponse<de.meply.meply.data.collection.BoardgameSearchResult>>

    /**
     * Create a new boardgame directly in Strapi
     * Uses standard Strapi POST /boardgames endpoint with data wrapper
     * This is step 1 of the two-step create-and-add process (like the web version)
     *
     * IMPORTANT: Uses explicit system token (like web version's useSystemToken=true)
     * because regular users don't have permission to create boardgames directly
     */
    @POST("boardgames")
    fun createBoardgameWithSystemToken(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body request: de.meply.meply.data.collection.StrapiCreateBoardgameRequest
    ): Call<de.meply.meply.data.collection.StrapiCreateBoardgameResponse>

    /**
     * Add a boardgame to user's collection
     * This is step 2 of the two-step create-and-add process
     */
    @POST("user-boardgames/add-to-collection")
    fun addToCollection(
        @Body request: de.meply.meply.data.collection.AddToCollectionRequest
    ): Call<de.meply.meply.data.collection.AddToCollectionResponse>

    /**
     * Update a user boardgame entry (rating, state, sale info)
     * @param entryId The user-boardgame entry ID
     */
    @PUT("user-boardgames/update/{entryId}")
    fun updateUserBoardgame(
        @Path("entryId") entryId: String,
        @Body request: de.meply.meply.data.collection.UpdateUserBoardgameRequest
    ): Call<de.meply.meply.data.collection.CollectionActionResponse>

    /**
     * Remove a boardgame from user's collection
     * @param entryId The user-boardgame entry ID
     */
    @DELETE("user-boardgames/remove/{entryId}")
    fun removeFromCollection(
        @Path("entryId") entryId: String
    ): Call<de.meply.meply.data.collection.CollectionActionResponse>

    // ===== MARKETPLACE (TROEDELMARKT) ENDPOINTS =====

    /**
     * Get marketplace listings (games for sale)
     * @param page Page number (1-based)
     * @param pageSize Items per page
     * @param title Filter by title (optional)
     */
    @GET("user-boardgames/marketplace")
    fun getMarktplace(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("title") title: String? = null
    ): Call<de.meply.meply.data.markt.MarktplaceResponse>

    // ===== USER UPLOADS (MEINE BILDER) ENDPOINTS =====

    /**
     * Get all images uploaded by the current user
     * @param page Page number (1-based)
     * @param pageSize Items per page (max 100)
     */
    @GET("user-uploads/me")
    fun getMyUploads(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 24
    ): Call<de.meply.meply.data.uploads.UserUploadsResponse>

    /**
     * Delete an uploaded image owned by the current user
     * @param uploadId The user-upload entry ID (not the file ID)
     */
    @DELETE("user-uploads/own/{id}")
    fun deleteUserUpload(
        @Path("id") uploadId: Int
    ): Call<de.meply.meply.data.uploads.DeleteUploadResponse>

    // ===== LOCATIONS ENDPOINTS =====

    /**
     * Get all locations for the current user
     * Uses filters to get only the user's own locations
     * @param authorDocumentId The author's profile document ID
     * @param publicationState "preview" to get both drafts and published
     */
    @GET("locations")
    fun getMyLocations(
        @Query("filters[author][documentId][\$eq]") authorDocumentId: String,
        @Query("publicationState") publicationState: String = "preview",
        @Query("populate") populate: String = "*"
    ): Call<de.meply.meply.data.locations.LocationsResponse>

    /**
     * Get a single location by document ID
     * @param documentId The location's document ID
     * @param publicationState "preview" to include drafts
     */
    @GET("locations/{documentId}")
    fun getLocation(
        @Path("documentId") documentId: String,
        @Query("publicationState") publicationState: String = "preview",
        @Query("populate") populate: String = "author"
    ): Call<de.meply.meply.data.locations.LocationSingleResponse>

    /**
     * Create a new location
     */
    @POST("locations")
    fun createLocation(
        @Body request: de.meply.meply.data.locations.CreateLocationRequest
    ): Call<de.meply.meply.data.locations.LocationActionResponse>

    /**
     * Update an existing location
     * @param documentId The location's document ID
     */
    @PUT("locations/{documentId}")
    fun updateLocation(
        @Path("documentId") documentId: String,
        @Body request: de.meply.meply.data.locations.CreateLocationRequest
    ): Call<de.meply.meply.data.locations.LocationActionResponse>

    /**
     * Delete a location
     * @param documentId The location's document ID
     */
    @DELETE("locations/{documentId}")
    fun deleteLocation(
        @Path("documentId") documentId: String
    ): Call<Void>

    /**
     * Discard draft changes for a location
     * @param documentId The location's document ID
     */
    @POST("locations/{documentId}/discard-draft")
    fun discardLocationDraft(
        @Path("documentId") documentId: String
    ): Call<Void>

    /**
     * Search locations (for autocomplete in forms)
     * @param query Search query (min 2 characters)
     * @param authorDocumentId To filter by owner
     */
    @GET("locations")
    fun searchLocations(
        @Query("filters[Titel][\$containsi]") query: String,
        @Query("filters[\$or][0][author][documentId]") authorDocumentId: String?,
        @Query("filters[\$or][1][allow_user_events]") allowUserEvents: Boolean = true,
        @Query("pagination[limit]") limit: Int = 5
    ): Call<de.meply.meply.data.locations.LocationsResponse>

    /**
     * Get all public locations (published)
     * @param type Optional filter by type (Geschäft, Cafe, Club, Location)
     * @param pageSize Number of results per page
     */
    @GET("locations/public")
    fun getPublicLocations(
        @Query("filters[Typ][\$eq]") type: String? = null,
        @Query("pagination[pageSize]") pageSize: Int = 100,
        @Query("populate") populate: String = "*"
    ): Call<de.meply.meply.data.locations.LocationsResponse>

    /**
     * Get locations nearby a postal code
     * @param zip Postal code to search around
     * @param radius Search radius in km
     * @param type Optional filter by type
     */
    @GET("locations/nearby")
    fun getNearbyLocations(
        @Query("zip") zip: String,
        @Query("radius") radius: Int,
        @Query("filters[Typ][\$eq]") type: String? = null,
        @Query("pagination[pageSize]") pageSize: Int = 100
    ): Call<de.meply.meply.data.locations.LocationsResponse>

    // ===== EVENTS ENDPOINTS =====

    /**
     * Get all events for the current user
     * Uses filters to get only the user's own events
     * @param authorDocumentId The author's profile document ID
     * @param publicationState "preview" to get both drafts and published
     */
    @GET("events")
    fun getMyEvents(
        @Query("filters[author][documentId][\$eq]") authorDocumentId: String,
        @Query("publicationState") publicationState: String = "preview",
        @Query("populate") populate: String = "*"
    ): Call<de.meply.meply.data.events.EventsResponse>

    /**
     * Get a single event by document ID
     * @param documentId The event's document ID
     * @param publicationState "preview" to include drafts
     */
    @GET("events/{documentId}")
    fun getEvent(
        @Path("documentId") documentId: String,
        @Query("publicationState") publicationState: String = "preview",
        @Query("populate[0]") populateAuthor: String = "author",
        @Query("populate[1]") populateLocation: String = "location"
    ): Call<de.meply.meply.data.events.EventSingleResponse>

    /**
     * Create a new event
     */
    @POST("events")
    fun createEvent(
        @Body request: de.meply.meply.data.events.CreateEventRequest
    ): Call<de.meply.meply.data.events.EventActionResponse>

    /**
     * Update an existing event
     * @param documentId The event's document ID
     */
    @PUT("events/{documentId}")
    fun updateEvent(
        @Path("documentId") documentId: String,
        @Body request: de.meply.meply.data.events.CreateEventRequest
    ): Call<de.meply.meply.data.events.EventActionResponse>

    /**
     * Delete an event
     * @param documentId The event's document ID
     */
    @DELETE("events/{documentId}")
    fun deleteEvent(
        @Path("documentId") documentId: String
    ): Call<Void>

    /**
     * Publish an event
     * @param documentId The event's document ID
     */
    @POST("events/{documentId}/publish")
    fun publishEvent(
        @Path("documentId") documentId: String
    ): Call<de.meply.meply.data.events.EventActionResponse>

    /**
     * Unpublish an event
     * @param documentId The event's document ID
     */
    @POST("events/{documentId}/unpublish")
    fun unpublishEvent(
        @Path("documentId") documentId: String
    ): Call<de.meply.meply.data.events.EventActionResponse>

    // ===== PLAYER MATCHING ENDPOINTS =====

    /**
     * Get best matching players nearby
     * @param profileId The current user's profile ID
     * @param limit Maximum number of results (default 20)
     */
    @GET("matches/best/{profileId}")
    fun getBestMatches(
        @Path("profileId") profileId: Int,
        @Query("limit") limit: Int = 20
    ): Call<List<de.meply.meply.data.players.PlayerMatch>>

    // ===== USER AVAILABILITY (SPIELBEREIT) ENDPOINTS =====

    /**
     * Get own availability status
     * Returns the current user's "ready to play" status
     */
    @GET("user-availabilities/my")
    fun getMyAvailability(): Call<de.meply.meply.data.availability.UserAvailabilityResponse>

    /**
     * Set or update own availability status
     * Creates a new availability or updates the existing one
     * @param request The availability data (expiresAt, hostingPreference, note, boardgames)
     */
    @POST("user-availabilities/my")
    fun setMyAvailability(
        @Body request: de.meply.meply.data.availability.SetAvailabilityRequest
    ): Call<de.meply.meply.data.availability.AvailabilityActionResponse>

    /**
     * Delete own availability status
     * Removes the "ready to play" status
     */
    @DELETE("user-availabilities/my")
    fun deleteMyAvailability(): Call<de.meply.meply.data.availability.AvailabilityActionResponse>

}
