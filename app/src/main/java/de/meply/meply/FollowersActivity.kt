package de.meply.meply

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.follower.*
import de.meply.meply.network.ApiClient
import de.meply.meply.utils.AvatarUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FollowersActivity : BaseDetailActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var pendingCard: MaterialCardView
    private lateinit var followersCard: MaterialCardView
    private lateinit var followingCard: MaterialCardView
    private lateinit var blockedCard: MaterialCardView

    private lateinit var pendingList: LinearLayout
    private lateinit var followersList: LinearLayout
    private lateinit var followingList: LinearLayout
    private lateinit var blockedList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)

        initializeViews()
        setupToolbar()
        setupSwipeRefresh()

        loadFollowerLists()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)

        pendingCard = findViewById(R.id.pendingCard)
        followersCard = findViewById(R.id.followersCard)
        followingCard = findViewById(R.id.followingCard)
        blockedCard = findViewById(R.id.blockedCard)

        pendingList = findViewById(R.id.pendingList)
        followersList = findViewById(R.id.followersList)
        followingList = findViewById(R.id.followingList)
        blockedList = findViewById(R.id.blockedList)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadFollowerLists()
        }
    }

    private fun loadFollowerLists() {
        val currentUserId = AuthManager.getUserDocumentId(this)
        if (currentUserId == null) {
            Toast.makeText(this, "Fehler: Benutzer nicht gefunden", Toast.LENGTH_SHORT).show()
            Log.e("FollowersActivity", "User documentId is null - check if it's saved on login")
            return
        }
        Log.d("FollowersActivity", "Loading follower lists for user: $currentUserId")

        progressBar.visibility = View.VISIBLE

        // Create a data holder to collect all 4 responses
        val listsData = FollowListResponse(
            pending = mutableListOf(),
            followers = mutableListOf(),
            following = mutableListOf(),
            blocked = mutableListOf()
        )
        var completedCalls = 0
        val totalCalls = 4

        // Helper function to check if all calls are done
        fun checkComplete() {
            completedCalls++
            if (completedCalls >= totalCalls) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                renderLists(listsData)
            }
        }

        // 1. Load pending requests (all -> currentUser with status=pending)
        ApiClient.retrofit.getFollowersByStatus("all", currentUserId, "pending")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        listsData.pending.addAll(users)
                        Log.d("FollowersActivity", "Loaded ${users.size} pending requests")
                    } else {
                        Log.e("FollowersActivity", "Error loading pending: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersActivity", "Error loading pending", t)
                    checkComplete()
                }
            })

        // 2. Load followers (all -> currentUser with status=accepted)
        ApiClient.retrofit.getFollowersByStatus("all", currentUserId, "accepted")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        listsData.followers.addAll(users)
                        Log.d("FollowersActivity", "Loaded ${users.size} followers")
                    } else {
                        Log.e("FollowersActivity", "Error loading followers: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersActivity", "Error loading followers", t)
                    checkComplete()
                }
            })

        // 3. Load following (currentUser -> all with status=accepted)
        ApiClient.retrofit.getFollowersByStatus(currentUserId, "all", "accepted")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        listsData.following.addAll(users)
                        Log.d("FollowersActivity", "Loaded ${users.size} following")
                    } else {
                        Log.e("FollowersActivity", "Error loading following: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersActivity", "Error loading following", t)
                    checkComplete()
                }
            })

        // 4. Load blocked (all -> currentUser with status=declined)
        ApiClient.retrofit.getFollowersByStatus("all", currentUserId, "declined")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        listsData.blocked.addAll(users)
                        Log.d("FollowersActivity", "Loaded ${users.size} blocked")
                    } else {
                        Log.e("FollowersActivity", "Error loading blocked: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersActivity", "Error loading blocked", t)
                    checkComplete()
                }
            })
    }

    private fun renderLists(data: FollowListResponse) {
        // Clear all lists
        pendingList.removeAllViews()
        followersList.removeAllViews()
        followingList.removeAllViews()
        blockedList.removeAllViews()

        Log.d("FollowersActivity", "Rendering lists - pending: ${data.pending.size}, followers: ${data.followers.size}, following: ${data.following.size}, blocked: ${data.blocked.size}")

        // Render pending requests
        val validPending = data.pending.filter { it.follower != null }
        if (validPending.size < data.pending.size) {
            Log.w("FollowersActivity", "Filtered out ${data.pending.size - validPending.size} null followers from pending")
        }
        if (validPending.isNotEmpty()) {
            pendingCard.visibility = View.VISIBLE
            validPending.forEach { relation ->
                // Safe access with ?: to handle nulls (should not happen after filter, but be safe)
                val user = relation.follower ?: return@forEach
                val itemView = createFollowerItem(
                    user = user,
                    type = ListType.PENDING,
                    followId = relation.documentId
                )
                pendingList.addView(itemView)
            }
        } else {
            pendingCard.visibility = View.GONE
        }

        // Render followers
        val validFollowers = data.followers.filter { it.follower != null }
        if (validFollowers.size < data.followers.size) {
            Log.w("FollowersActivity", "Filtered out ${data.followers.size - validFollowers.size} null followers from followers")
        }
        if (validFollowers.isNotEmpty()) {
            followersCard.visibility = View.VISIBLE
            validFollowers.forEach { relation ->
                val user = relation.follower ?: return@forEach
                val itemView = createFollowerItem(
                    user = user,
                    type = ListType.FOLLOWERS,
                    followId = relation.documentId
                )
                followersList.addView(itemView)
            }
        } else {
            followersCard.visibility = View.GONE
        }

        // Render following
        val validFollowing = data.following.filter { it.following != null }
        if (validFollowing.size < data.following.size) {
            Log.w("FollowersActivity", "Filtered out ${data.following.size - validFollowing.size} null following from following")
        }
        if (validFollowing.isNotEmpty()) {
            followingCard.visibility = View.VISIBLE
            validFollowing.forEach { relation ->
                val user = relation.following ?: return@forEach
                val itemView = createFollowerItem(
                    user = user,
                    type = ListType.FOLLOWING,
                    followId = relation.documentId,
                    userDocumentId = user.documentId
                )
                followingList.addView(itemView)
            }
        } else {
            followingCard.visibility = View.GONE
        }

        // Render blocked
        val validBlocked = data.blocked.filter { it.follower != null }
        if (validBlocked.size < data.blocked.size) {
            Log.w("FollowersActivity", "Filtered out ${data.blocked.size - validBlocked.size} null followers from blocked")
        }
        if (validBlocked.isNotEmpty()) {
            blockedCard.visibility = View.VISIBLE
            validBlocked.forEach { relation ->
                val user = relation.follower ?: return@forEach
                val itemView = createFollowerItem(
                    user = user,
                    type = ListType.BLOCKED,
                    followId = relation.documentId
                )
                blockedList.addView(itemView)
            }
        } else {
            blockedCard.visibility = View.GONE
        }
    }

    private fun createFollowerItem(
        user: FollowerUser,
        type: ListType,
        followId: String,
        userDocumentId: String? = null
    ): View {
        val parentLayout = when (type) {
            ListType.PENDING -> pendingList
            ListType.FOLLOWERS -> followersList
            ListType.FOLLOWING -> followingList
            ListType.BLOCKED -> blockedList
        }

        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.list_item_follower, parentLayout, false)

        val avatar = itemView.findViewById<ImageView>(R.id.userAvatar)
        val username = itemView.findViewById<TextView>(R.id.username)
        val btnAccept = itemView.findViewById<MaterialButton>(R.id.btnAccept)
        val btnDecline = itemView.findViewById<MaterialButton>(R.id.btnDecline)
        val btnUnfollow = itemView.findViewById<MaterialButton>(R.id.btnUnfollow)

        // Set username
        username.text = user.username

        // Load avatar
        val avatarUrl = user.avatar?.firstOrNull()?.url
        if (!avatarUrl.isNullOrEmpty()) {
            val fullUrl = "${ApiClient.STRAPI_IMAGE_BASE}$avatarUrl"
            Glide.with(this)
                .load(fullUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(avatar)
        } else {
            val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(user.documentId)
            Glide.with(this)
                .load(defaultAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(avatar)
        }

        // Configure buttons based on list type
        when (type) {
            ListType.PENDING -> {
                btnAccept.visibility = View.VISIBLE
                btnDecline.visibility = View.VISIBLE
                btnAccept.text = "Akzeptieren"
                btnDecline.text = "Ablehnen"

                btnAccept.setOnClickListener {
                    manageFollow(followId, "accepted", itemView)
                }
                btnDecline.setOnClickListener {
                    manageFollow(followId, "declined", itemView)
                }
            }
            ListType.FOLLOWERS -> {
                btnDecline.visibility = View.VISIBLE
                btnDecline.text = "Entfernen"

                btnDecline.setOnClickListener {
                    manageFollow(followId, "declined", itemView)
                }
            }
            ListType.FOLLOWING -> {
                btnUnfollow.visibility = View.VISIBLE

                btnUnfollow.setOnClickListener {
                    if (userDocumentId != null) {
                        toggleFollow(userDocumentId, itemView)
                    }
                }
            }
            ListType.BLOCKED -> {
                btnAccept.visibility = View.VISIBLE
                btnAccept.text = "Akzeptieren"

                btnAccept.setOnClickListener {
                    manageFollow(followId, "accepted", itemView)
                }
            }
        }

        return itemView
    }

    private fun manageFollow(followId: String, action: String, itemView: View) {
        val request = FollowManageRequest(action)

        ApiClient.retrofit.manageFollow(followId, request)
            .enqueue(object : Callback<FollowManageResponse> {
                override fun onResponse(
                    call: Call<FollowManageResponse>,
                    response: Response<FollowManageResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Reload lists to reflect changes
                        loadFollowerLists()
                    } else {
                        Toast.makeText(
                            this@FollowersActivity,
                            "Fehler bei der Aktion",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FollowManageResponse>, t: Throwable) {
                    Toast.makeText(
                        this@FollowersActivity,
                        "Netzwerkfehler",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FollowersActivity", "Error managing follow", t)
                }
            })
    }

    private fun toggleFollow(userDocumentId: String, itemView: View) {
        val request = FollowToggleRequest(userDocumentId)

        ApiClient.retrofit.toggleFollow(request)
            .enqueue(object : Callback<FollowToggleResponse> {
                override fun onResponse(
                    call: Call<FollowToggleResponse>,
                    response: Response<FollowToggleResponse>
                ) {
                    if (response.isSuccessful) {
                        // Reload lists to reflect changes
                        loadFollowerLists()
                    } else {
                        Toast.makeText(
                            this@FollowersActivity,
                            "Fehler beim Entfolgen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FollowToggleResponse>, t: Throwable) {
                    Toast.makeText(
                        this@FollowersActivity,
                        "Netzwerkfehler",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FollowersActivity", "Error toggling follow", t)
                }
            })
    }

    private enum class ListType {
        PENDING,
        FOLLOWERS,
        FOLLOWING,
        BLOCKED
    }
}
