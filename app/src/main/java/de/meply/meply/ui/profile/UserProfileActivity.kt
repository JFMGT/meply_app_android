package de.meply.meply.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import de.meply.meply.BaseDetailActivity
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.data.feed.FeedResponse
import de.meply.meply.data.feed.Post
import de.meply.meply.data.meetings.MeetingData
import de.meply.meply.data.messages.CreateConversationRequest
import de.meply.meply.data.messages.SendMessageResponse
import de.meply.meply.data.profile.*
import de.meply.meply.data.follower.FollowToggleRequest
import de.meply.meply.data.follower.FollowToggleResponse
import de.meply.meply.network.ApiClient
import de.meply.meply.ui.events.MeetingsAdapter
import de.meply.meply.ui.feed.FeedAdapter
import de.meply.meply.utils.AvatarUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProfileActivity : BaseDetailActivity() {

    private lateinit var profileAvatar: ImageView
    private lateinit var profileUsername: TextView
    private lateinit var profileDetails: TextView
    private lateinit var profileLocation: TextView
    private lateinit var btnSendMessage: Button
    private lateinit var statsCard: MaterialCardView
    private lateinit var noStatsMessage: TextView
    private lateinit var matchScore: TextView
    private lateinit var sharedCount: TextView
    private lateinit var distance: TextView
    private lateinit var btnFollow: Button
    private lateinit var btnTabComparison: Button
    private lateinit var btnTabMeetings: Button
    private lateinit var btnTabSales: Button
    private lateinit var btnTabPosts: Button
    private lateinit var tabComparison: View
    private lateinit var tabMeetings: View
    private lateinit var tabSales: View
    private lateinit var tabPosts: View
    private lateinit var mainProgressBar: ProgressBar

    // Comparison tab elements
    private lateinit var externalProfilesCard: MaterialCardView
    private lateinit var linkBgg: TextView
    private lateinit var linkBga: TextView
    private lateinit var sharedGamesProgress: ProgressBar
    private lateinit var sharedGamesMessage: TextView
    private lateinit var sharedGamesRecycler: RecyclerView
    private lateinit var sharedGamesAdapter: SharedGamesAdapter

    // Flea market / Flohmarkt elements
    private lateinit var salesProgress: ProgressBar
    private lateinit var salesEmptyMessage: TextView
    private lateinit var salesListContainer: LinearLayout

    // Meetings tab elements
    private lateinit var meetingsProgress: ProgressBar
    private lateinit var meetingsEmptyMessage: TextView
    private lateinit var meetingsRecycler: RecyclerView
    private lateinit var meetingsAdapter: MeetingsAdapter

    // Posts tab elements
    private lateinit var postsProgress: ProgressBar
    private lateinit var postsEmptyCard: MaterialCardView
    private lateinit var postsEmptyMessage: TextView
    private lateinit var postsRecycler: RecyclerView
    private lateinit var postsAdapter: FeedAdapter
    private val userPosts: MutableList<Post> = mutableListOf()

    private var userSlug: String? = null
    private var profileData: UserProfileData? = null
    private var currentUserId: String? = null
    private var currentUserProfileId: Int? = null
    private var isFollowing: Boolean = false
    private var salesLoaded: Boolean = false
    private var postsLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        userSlug = intent.getStringExtra(EXTRA_USER_SLUG)
        if (userSlug == null) {
            Toast.makeText(this, "Fehler: Benutzer nicht gefunden", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get current user IDs
        currentUserId = AuthManager.getUserDocumentId(this)
        currentUserProfileId = AuthManager.getProfileId(this)

        initializeViews()
        setupDetailToolbar()
        setupTabs()
        setupAdapters()

        loadUserProfile()
    }

    private fun initializeViews() {
        profileAvatar = findViewById(R.id.profile_avatar)
        profileUsername = findViewById(R.id.profile_username)
        profileDetails = findViewById(R.id.profile_details)
        profileLocation = findViewById(R.id.profile_location)
        btnSendMessage = findViewById(R.id.btn_send_message)
        statsCard = findViewById(R.id.stats_card)
        noStatsMessage = findViewById(R.id.no_stats_message)
        matchScore = findViewById(R.id.match_score)
        sharedCount = findViewById(R.id.shared_count)
        distance = findViewById(R.id.distance)
        btnFollow = findViewById(R.id.btn_follow)
        btnTabComparison = findViewById(R.id.btn_tab_comparison)
        btnTabMeetings = findViewById(R.id.btn_tab_meetings)
        btnTabSales = findViewById(R.id.btn_tab_sales)
        btnTabPosts = findViewById(R.id.btn_tab_posts)
        tabComparison = findViewById(R.id.tab_comparison)
        tabMeetings = findViewById(R.id.tab_meetings)
        tabSales = findViewById(R.id.tab_sales)
        tabPosts = findViewById(R.id.tab_posts)
        mainProgressBar = findViewById(R.id.main_progress_bar)

        // Comparison tab
        externalProfilesCard = findViewById(R.id.external_profiles_card)
        linkBgg = findViewById(R.id.link_bgg)
        linkBga = findViewById(R.id.link_bga)
        sharedGamesProgress = findViewById(R.id.shared_games_progress)
        sharedGamesMessage = findViewById(R.id.shared_games_message)
        sharedGamesRecycler = findViewById(R.id.shared_games_recycler)

        // Flea market / Flohmarkt
        salesProgress = findViewById(R.id.sales_progress)
        salesEmptyMessage = findViewById(R.id.sales_empty_message)
        salesListContainer = findViewById(R.id.sales_list_container)

        // Meetings tab
        meetingsProgress = findViewById(R.id.meetings_progress)
        meetingsEmptyMessage = findViewById(R.id.meetings_empty_message)
        meetingsRecycler = findViewById(R.id.meetings_recycler)

        // Posts tab
        postsProgress = findViewById(R.id.posts_progress)
        postsEmptyCard = findViewById(R.id.posts_empty_card)
        postsEmptyMessage = findViewById(R.id.posts_empty_message)
        postsRecycler = findViewById(R.id.posts_recycler)

        btnSendMessage.setOnClickListener { onSendMessage() }
        btnFollow.setOnClickListener { onFollowClick() }
    }

    private fun setupTabs() {
        btnTabComparison.setOnClickListener {
            showTab(TAB_COMPARISON)
        }
        btnTabMeetings.setOnClickListener {
            showTab(TAB_MEETINGS)
        }
        btnTabSales.setOnClickListener {
            showTab(TAB_SALES)
        }
        btnTabPosts.setOnClickListener {
            showTab(TAB_POSTS)
        }
        showTab(TAB_COMPARISON)
    }

    private fun showTab(tab: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = android.graphics.Color.TRANSPARENT

        // Reset all tabs
        tabComparison.visibility = View.GONE
        tabMeetings.visibility = View.GONE
        tabSales.visibility = View.GONE
        tabPosts.visibility = View.GONE
        btnTabComparison.isEnabled = true
        btnTabMeetings.isEnabled = true
        btnTabSales.isEnabled = true
        btnTabPosts.isEnabled = true
        btnTabComparison.setBackgroundColor(inactiveColor)
        btnTabMeetings.setBackgroundColor(inactiveColor)
        btnTabSales.setBackgroundColor(inactiveColor)
        btnTabPosts.setBackgroundColor(inactiveColor)

        when (tab) {
            TAB_COMPARISON -> {
                tabComparison.visibility = View.VISIBLE
                btnTabComparison.isEnabled = false
                btnTabComparison.setBackgroundColor(activeColor)
            }
            TAB_MEETINGS -> {
                tabMeetings.visibility = View.VISIBLE
                btnTabMeetings.isEnabled = false
                btnTabMeetings.setBackgroundColor(activeColor)

                // Load meetings when tab is shown for the first time
                if (meetingsAdapter.itemCount == 0) {
                    loadUserMeetings()
                }
            }
            TAB_SALES -> {
                tabSales.visibility = View.VISIBLE
                btnTabSales.isEnabled = false
                btnTabSales.setBackgroundColor(activeColor)

                // Load sales when tab is shown for the first time
                if (!salesLoaded) {
                    loadUserSales()
                }
            }
            TAB_POSTS -> {
                tabPosts.visibility = View.VISIBLE
                btnTabPosts.isEnabled = false
                btnTabPosts.setBackgroundColor(activeColor)

                // Load posts when tab is shown for the first time
                if (!postsLoaded) {
                    loadUserPosts()
                }
            }
        }
    }

    private fun setupAdapters() {
        sharedGamesAdapter = SharedGamesAdapter()
        sharedGamesRecycler.layoutManager = LinearLayoutManager(this)
        sharedGamesRecycler.adapter = sharedGamesAdapter

        meetingsAdapter = MeetingsAdapter(
            onContactClick = { meeting -> onContactMeeting(meeting) },
            onAuthorClick = { userSlug -> openUserProfile(userSlug) }
        )
        meetingsRecycler.layoutManager = LinearLayoutManager(this)
        meetingsRecycler.adapter = meetingsAdapter

        postsAdapter = FeedAdapter(
            posts = userPosts,
            onLikeClick = { /* Not implemented in profile view */ },
            onReplyClick = { /* Not implemented in profile view */ },
            onShowRepliesClick = { /* Not implemented in profile view */ },
            onOptionsClick = { _, _ -> /* Not implemented in profile view */ },
            onImageClick = { _, _ -> /* Not implemented in profile view */ },
            onAuthorClick = null // Already viewing their profile
        )
        postsRecycler.layoutManager = LinearLayoutManager(this)
        postsRecycler.adapter = postsAdapter
    }

    private fun openUserProfile(userSlug: String) {
        UserProfileActivity.start(this, userSlug)
    }

    private fun loadUserProfile() {
        mainProgressBar.visibility = View.VISIBLE

        ApiClient.retrofit.getUserProfileBySlug(userSlug!!).enqueue(object : Callback<UserProfileResponse> {
            override fun onResponse(
                call: Call<UserProfileResponse>,
                response: Response<UserProfileResponse>
            ) {
                mainProgressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    profileData = response.body()?.data
                    if (profileData != null) {
                        displayProfile(profileData!!)
                        loadMatchScore()
                        loadFollowStatus()
                        loadSharedGames()
                    } else {
                        showError("Profil nicht gefunden")
                    }
                } else if (response.code() == 204) {
                    showError("Dieses Profil ist privat")
                } else {
                    showError("Fehler beim Laden: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserProfileResponse>, t: Throwable) {
                mainProgressBar.visibility = View.GONE
                showError("Netzwerkfehler: ${t.message}")
                Log.e(TAG, "Error loading profile", t)
            }
        })
    }

    private fun displayProfile(profile: UserProfileData) {
        // Username
        profileUsername.text = profile.username ?: "Unbekannt"

        // Details (age + gender)
        val details = buildString {
            val age = getAgeFromBirthDate(profile.birthDate)
            if (age != null) {
                append(age)
            }
            val genderIcon = getGenderIcon(profile.gender)
            if (genderIcon != null) {
                if (isNotEmpty()) append(", ")
                append(genderIcon)
            }
        }
        if (details.isNotEmpty()) {
            profileDetails.text = details
            profileDetails.visibility = View.VISIBLE
        } else {
            profileDetails.visibility = View.GONE
        }

        // Location
        val location = buildString {
            if (!profile.postalCode.isNullOrEmpty() || !profile.city.isNullOrEmpty()) {
                append("📍 ")
                append(profile.postalCode?.trim() ?: "")
                if (!profile.postalCode.isNullOrEmpty() && !profile.city.isNullOrEmpty()) {
                    append(" ")
                }
                append(profile.city?.trim() ?: "")
            }
        }
        if (location.isNotEmpty()) {
            profileLocation.text = location
            profileLocation.visibility = View.VISIBLE
        } else {
            profileLocation.visibility = View.GONE
        }

        // Avatar
        loadAvatar(profile)

        // External profiles
        setupExternalProfiles(profile)
    }

    private fun loadAvatar(profile: UserProfileData) {
        if (!profile.avatar.isNullOrEmpty()) {
            // User has uploaded avatar
            val avatarUrl = ApiClient.STRAPI_IMAGE_BASE + profile.avatar[0].url
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.rounded_corner_background)
                .error(R.drawable.rounded_corner_background)
                .into(profileAvatar)
        } else {
            // Generate default avatar based on userId (matching PHP implementation)
            val userId = profile.userDocumentId ?: profile.documentId ?: "default"
            val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(userId)
            Glide.with(this)
                .load(defaultAvatarUrl)
                .placeholder(R.drawable.rounded_corner_background)
                .error(R.drawable.rounded_corner_background)
                .into(profileAvatar)
        }
    }

    private fun setupExternalProfiles(profile: UserProfileData) {
        var hasExternalProfile = false

        if (!profile.boardgamegeekProfile.isNullOrEmpty()) {
            linkBgg.visibility = View.VISIBLE
            linkBgg.setOnClickListener {
                openUrl(profile.boardgamegeekProfile!!)
            }
            hasExternalProfile = true
        } else {
            linkBgg.visibility = View.GONE
        }

        if (!profile.boardGameArenaProfile.isNullOrEmpty()) {
            linkBga.visibility = View.VISIBLE
            linkBga.setOnClickListener {
                openUrl(profile.boardGameArenaProfile!!)
            }
            hasExternalProfile = true
        }

        externalProfilesCard.visibility = if (hasExternalProfile) View.VISIBLE else View.GONE
    }

    private fun loadMatchScore() {
        if (currentUserProfileId == null) {
            Log.e(TAG, "Current user profile ID is null")
            return
        }

        val profileId = profileData?.id ?: return

        ApiClient.retrofit.getMatchScore(currentUserProfileId!!.toString(), profileId.toString())
            .enqueue(object : Callback<MatchScoreResponse> {
                override fun onResponse(
                    call: Call<MatchScoreResponse>,
                    response: Response<MatchScoreResponse>
                ) {
                    if (response.isSuccessful) {
                        val matchData = response.body()
                        if (matchData != null && matchData.score != null) {
                            displayMatchScore(matchData)
                        } else {
                            showNoStatsMessage()
                        }
                    } else {
                        Log.e(TAG, "Error loading match score: ${response.code()}")
                        showNoStatsMessage()
                    }
                }

                override fun onFailure(call: Call<MatchScoreResponse>, t: Throwable) {
                    Log.e(TAG, "Error loading match score", t)
                    showNoStatsMessage()
                }
            })
    }

    private fun displayMatchScore(matchData: MatchScoreResponse) {
        statsCard.visibility = View.VISIBLE
        noStatsMessage.visibility = View.GONE

        matchScore.text = "${(matchData.score!! * 100).toInt()}%"
        sharedCount.text = matchData.sharedCount?.toString() ?: "0"
        distance.text = "${matchData.distance?.toInt() ?: 0} km"
    }

    private fun showNoStatsMessage() {
        statsCard.visibility = View.GONE
        noStatsMessage.visibility = View.VISIBLE
    }

    private fun loadFollowStatus() {
        val userDocId = profileData?.userDocumentId ?: return
        if (currentUserId.isNullOrEmpty()) return

        ApiClient.retrofit.checkFollowStatus(currentUserId!!, userDocId)
            .enqueue(object : Callback<FollowStatusResponse> {
                override fun onResponse(
                    call: Call<FollowStatusResponse>,
                    response: Response<FollowStatusResponse>
                ) {
                    if (response.isSuccessful) {
                        isFollowing = response.body()?.isFollowing ?: false
                        updateFollowButton()
                    }
                }

                override fun onFailure(call: Call<FollowStatusResponse>, t: Throwable) {
                    Log.e(TAG, "Error loading follow status", t)
                }
            })
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            btnFollow.text = "➖ Entfolgen"
        } else {
            btnFollow.text = "➕ Folgen"
        }
    }

    private fun loadSharedGames() {
        if (currentUserProfileId == null) return
        val profileId = profileData?.id ?: return

        sharedGamesProgress.visibility = View.VISIBLE
        sharedGamesMessage.visibility = View.GONE
        sharedGamesRecycler.visibility = View.GONE

        ApiClient.retrofit.getSharedHighlyRatedGames(currentUserProfileId!!.toString(), profileId.toString())
            .enqueue(object : Callback<List<SharedGame>> {
                override fun onResponse(
                    call: Call<List<SharedGame>>,
                    response: Response<List<SharedGame>>
                ) {
                    sharedGamesProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        val games = response.body()
                        if (!games.isNullOrEmpty()) {
                            // Take max 5 games
                            sharedGamesAdapter.submitList(games.take(5))
                            sharedGamesRecycler.visibility = View.VISIBLE
                            sharedGamesMessage.visibility = View.GONE
                        } else {
                            sharedGamesMessage.visibility = View.VISIBLE
                            sharedGamesRecycler.visibility = View.GONE
                        }
                    } else {
                        sharedGamesMessage.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: Call<List<SharedGame>>, t: Throwable) {
                    sharedGamesProgress.visibility = View.GONE
                    sharedGamesMessage.visibility = View.VISIBLE
                    Log.e(TAG, "Error loading shared games", t)
                }
            })
    }

    private fun loadUserSales() {
        val profileId = profileData?.id ?: return

        salesProgress.visibility = View.VISIBLE
        salesEmptyMessage.visibility = View.GONE
        salesListContainer.visibility = View.GONE

        ApiClient.retrofit.getUserSales(profileId.toString())
            .enqueue(object : Callback<UserSalesResponse> {
                override fun onResponse(
                    call: Call<UserSalesResponse>,
                    response: Response<UserSalesResponse>
                ) {
                    salesProgress.visibility = View.GONE
                    salesLoaded = true

                    if (response.isSuccessful) {
                        val sales = response.body()?.sales
                        if (!sales.isNullOrEmpty()) {
                            displayUserSales(sales)
                        } else {
                            salesEmptyMessage.visibility = View.VISIBLE
                        }
                    } else {
                        salesEmptyMessage.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: Call<UserSalesResponse>, t: Throwable) {
                    salesProgress.visibility = View.GONE
                    salesEmptyMessage.visibility = View.VISIBLE
                    Log.e(TAG, "Error loading user sales", t)
                }
            })
    }

    private fun displayUserSales(sales: List<UserSaleItem>) {
        salesListContainer.removeAllViews()
        salesListContainer.visibility = View.VISIBLE
        salesEmptyMessage.visibility = View.GONE

        for (sale in sales) {
            val itemView = layoutInflater.inflate(R.layout.item_user_sale, salesListContainer, false)

            val titleText = itemView.findViewById<TextView>(R.id.sale_title)
            val detailsText = itemView.findViewById<TextView>(R.id.sale_details)

            titleText.text = sale.title ?: "Unbekanntes Spiel"

            val details = buildString {
                append(sale.getFormattedPrice())
                val condition = sale.getLocalizedCondition()
                if (condition.isNotEmpty()) {
                    append(" • ")
                    append(condition)
                }
                val delivery = sale.getLocalizedDeliveryOption()
                if (delivery.isNotEmpty()) {
                    append(" • ")
                    append(delivery)
                }
                if (sale.tradePossible == true) {
                    append(" 🔁")
                }
            }
            detailsText.text = details

            salesListContainer.addView(itemView)
        }
    }

    private fun loadUserPosts() {
        val authorDocId = profileData?.documentId ?: return

        postsProgress.visibility = View.VISIBLE
        postsEmptyCard.visibility = View.GONE
        postsRecycler.visibility = View.GONE

        ApiClient.retrofit.getFeed(limit = 20, author = authorDocId)
            .enqueue(object : Callback<FeedResponse> {
                override fun onResponse(
                    call: Call<FeedResponse>,
                    response: Response<FeedResponse>
                ) {
                    postsProgress.visibility = View.GONE
                    postsLoaded = true

                    if (response.isSuccessful) {
                        val posts = response.body()?.feed
                        if (!posts.isNullOrEmpty()) {
                            userPosts.clear()
                            userPosts.addAll(posts)
                            postsAdapter.notifyDataSetChanged()
                            postsRecycler.visibility = View.VISIBLE
                        } else {
                            postsEmptyCard.visibility = View.VISIBLE
                        }
                    } else {
                        postsEmptyCard.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: Call<FeedResponse>, t: Throwable) {
                    postsProgress.visibility = View.GONE
                    postsEmptyCard.visibility = View.VISIBLE
                    Log.e(TAG, "Error loading user posts", t)
                }
            })
    }

    private fun loadUserMeetings() {
        val profileDocId = profileData?.documentId ?: return

        meetingsProgress.visibility = View.VISIBLE
        meetingsEmptyMessage.visibility = View.GONE
        meetingsRecycler.visibility = View.GONE

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        ApiClient.retrofit.getMeetingsByAuthor(
            authorDocumentId = profileDocId,
            dateIsGte = today
        ).enqueue(object : Callback<StrapiListResponse<MeetingData>> {
            override fun onResponse(
                call: Call<StrapiListResponse<MeetingData>>,
                response: Response<StrapiListResponse<MeetingData>>
            ) {
                meetingsProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    val userMeetings = response.body()?.data ?: emptyList()

                    if (userMeetings.isNotEmpty()) {
                        meetingsAdapter.submitList(userMeetings)
                        meetingsRecycler.visibility = View.VISIBLE
                    } else {
                        meetingsEmptyMessage.visibility = View.VISIBLE
                    }
                } else {
                    meetingsEmptyMessage.visibility = View.VISIBLE
                    Log.e(TAG, "Error loading meetings: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<StrapiListResponse<MeetingData>>, t: Throwable) {
                meetingsProgress.visibility = View.GONE
                meetingsEmptyMessage.visibility = View.VISIBLE
                Log.e(TAG, "Error loading meetings", t)
            }
        })
    }

    private fun onSendMessage() {
        val recipientDocId = profileData?.documentId ?: return
        val recipientName = profileData?.username ?: "diesem Nutzer"

        val messageInput = EditText(this)
        messageInput.hint = "Deine Nachricht"
        messageInput.maxLines = 5

        AlertDialog.Builder(this)
            .setTitle("Nachricht an $recipientName")
            .setView(messageInput)
            .setPositiveButton("Senden") { _, _ ->
                val messageText = messageInput.text.toString().trim()
                if (messageText.isEmpty()) {
                    Toast.makeText(this, "Bitte eine Nachricht eingeben", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (messageText.length > 500) {
                    Toast.makeText(this, "Nachricht zu lang (max. 500 Zeichen)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendMessage(recipientDocId, messageText, recipientName)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun sendMessage(recipientId: String, message: String, recipientName: String) {
        val request = CreateConversationRequest(
            recipient = recipientId,
            message = message
        )

        ApiClient.retrofit.createConversation(request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(
                call: Call<SendMessageResponse>,
                response: Response<SendMessageResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Nachricht an $recipientName gesendet!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Fehler beim Senden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                Toast.makeText(
                    this@UserProfileActivity,
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun onFollowClick() {
        val userDocId = profileData?.userDocumentId ?: return
        toggleFollowUser(userDocId)
    }

    private fun toggleFollowUser(userDocId: String) {
        val request = FollowToggleRequest(documentId = userDocId)

        ApiClient.retrofit.toggleFollow(request).enqueue(object : Callback<FollowToggleResponse> {
            override fun onResponse(
                call: Call<FollowToggleResponse>,
                response: Response<FollowToggleResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    when (result?.status) {
                        "followed" -> {
                            isFollowing = true
                            updateFollowButton()
                            Toast.makeText(this@UserProfileActivity, "Du folgst jetzt ${profileData?.username}", Toast.LENGTH_SHORT).show()
                        }
                        "unfollowed" -> {
                            isFollowing = false
                            updateFollowButton()
                            Toast.makeText(this@UserProfileActivity, "Du folgst ${profileData?.username} nicht mehr", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Fallback: toggle the local state
                            isFollowing = !isFollowing
                            updateFollowButton()
                        }
                    }
                } else {
                    Toast.makeText(this@UserProfileActivity, "Fehler: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FollowToggleResponse>, t: Throwable) {
                Toast.makeText(this@UserProfileActivity, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun onContactMeeting(meeting: MeetingData) {
        val authorDocumentId = meeting.author?.documentId
        val authorName = meeting.author?.username ?: "diesem Nutzer"

        if (authorDocumentId.isNullOrBlank()) {
            Toast.makeText(this, "Autor-ID nicht verfügbar", Toast.LENGTH_SHORT).show()
            return
        }

        val messageInput = EditText(this)
        messageInput.hint = "Deine Nachricht"
        messageInput.maxLines = 5

        AlertDialog.Builder(this)
            .setTitle("Nachricht an $authorName")
            .setMessage("Schreibe eine Nachricht zum Spieltreffen \"${meeting.title ?: "Spieltreffen"}\"")
            .setView(messageInput)
            .setPositiveButton("Senden") { _, _ ->
                val messageText = messageInput.text.toString().trim()
                if (messageText.isEmpty()) {
                    Toast.makeText(this, "Bitte eine Nachricht eingeben", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (messageText.length > 500) {
                    Toast.makeText(this, "Nachricht zu lang (max. 500 Zeichen)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendMeetingMessage(authorDocumentId, messageText, meeting.documentId, authorName)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun sendMeetingMessage(recipientId: String, message: String, meetingReference: String?, recipientName: String) {
        val request = CreateConversationRequest(
            recipient = recipientId,
            message = message,
            reference = meetingReference
        )

        ApiClient.retrofit.createConversation(request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(
                call: Call<SendMessageResponse>,
                response: Response<SendMessageResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Nachricht an $recipientName gesendet!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Fehler beim Senden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                Toast.makeText(
                    this@UserProfileActivity,
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Konnte URL nicht öffnen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAgeFromBirthDate(birthDate: String?): Int? {
        if (birthDate.isNullOrEmpty()) return null

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birth = sdf.parse(birthDate) ?: return null
            val today = Date()
            val diff = today.time - birth.time
            val age = (diff / (1000L * 60 * 60 * 24 * 365.25)).toInt()
            age
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing birth date", e)
            null
        }
    }

    private fun getGenderIcon(gender: String?): String? {
        return when (gender) {
            "male" -> "♂"
            "female" -> "♀"
            "diverse" -> "⚧"
            "trans" -> "⚧"
            "other" -> "🦦"
            else -> null
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        profileUsername.text = message
    }

    companion object {
        private const val TAG = "UserProfileActivity"
        private const val EXTRA_USER_SLUG = "user_slug"
        private const val TAB_COMPARISON = 0
        private const val TAB_MEETINGS = 1
        private const val TAB_SALES = 2
        private const val TAB_POSTS = 3

        fun start(context: Context, userSlug: String) {
            val intent = Intent(context, UserProfileActivity::class.java).apply {
                putExtra(EXTRA_USER_SLUG, userSlug)
            }
            context.startActivity(intent)
        }
    }
}
