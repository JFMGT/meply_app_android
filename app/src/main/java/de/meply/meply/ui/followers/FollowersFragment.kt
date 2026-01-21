package de.meply.meply.ui.followers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.follower.*
import de.meply.meply.network.ApiClient
import de.meply.meply.utils.AvatarUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FollowersFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var viewPager: ViewPager2
    private lateinit var tabDots: TabLayout
    private lateinit var pagerAdapter: FollowerPagerAdapter

    private val pageContainers = mutableMapOf<FollowerListType, LinearLayout>()
    private var listsData: FollowListResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_followers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupViewPager()
        setupSwipeRefresh()

        loadFollowerLists()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        viewPager = view.findViewById(R.id.viewPager)
        tabDots = view.findViewById(R.id.tabDots)
    }

    private fun setupViewPager() {
        pagerAdapter = FollowerPagerAdapter()
        pagerAdapter.onPageCreated = { pageView, type ->
            val container = pageView.findViewById<LinearLayout>(R.id.itemsContainer)
            pageContainers[type] = container
            // Render data if already loaded
            listsData?.let { renderPageData(type, it) }
        }

        viewPager.adapter = pagerAdapter

        // Setup tabs with labels
        TabLayoutMediator(tabDots, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Anfragen"
                1 -> "Follower"
                2 -> "Folge ich"
                3 -> "Blockiert"
                else -> ""
            }
        }.attach()
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadFollowerLists()
        }
    }

    private fun loadFollowerLists() {
        val currentUserId = AuthManager.getUserDocumentId(requireContext())
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Fehler: Benutzer nicht gefunden", Toast.LENGTH_SHORT).show()
            Log.e("FollowersFragment", "User documentId is null - check if it's saved on login")
            return
        }
        Log.d("FollowersFragment", "Loading follower lists for user: $currentUserId")

        progressBar.visibility = View.VISIBLE

        // Create a data holder to collect all 4 responses
        val data = FollowListResponse(
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
                listsData = data
                renderAllPages(data)
            }
        }

        // 1. Load pending requests
        ApiClient.retrofit.getFollowersByStatus("all", currentUserId, "pending")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        data.pending.addAll(users)
                        Log.d("FollowersFragment", "Loaded ${users.size} pending requests")
                    } else {
                        Log.e("FollowersFragment", "Error loading pending: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersFragment", "Error loading pending", t)
                    checkComplete()
                }
            })

        // 2. Load followers
        ApiClient.retrofit.getFollowersByStatus("all", currentUserId, "accepted")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        data.followers.addAll(users)
                        Log.d("FollowersFragment", "Loaded ${users.size} followers")
                    } else {
                        Log.e("FollowersFragment", "Error loading followers: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersFragment", "Error loading followers", t)
                    checkComplete()
                }
            })

        // 3. Load following
        ApiClient.retrofit.getFollowersByStatus(currentUserId, "all", "accepted")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        data.following.addAll(users)
                        Log.d("FollowersFragment", "Loaded ${users.size} following")
                    } else {
                        Log.e("FollowersFragment", "Error loading following: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersFragment", "Error loading following", t)
                    checkComplete()
                }
            })

        // 4. Load blocked
        ApiClient.retrofit.getFollowersByStatus("all", currentUserId, "declined")
            .enqueue(object : Callback<FollowedByResponse> {
                override fun onResponse(
                    call: Call<FollowedByResponse>,
                    response: Response<FollowedByResponse>
                ) {
                    if (response.isSuccessful) {
                        val users = response.body()?.users ?: emptyList()
                        data.blocked.addAll(users)
                        Log.d("FollowersFragment", "Loaded ${users.size} blocked")
                    } else {
                        Log.e("FollowersFragment", "Error loading blocked: ${response.code()} - ${response.message()}")
                    }
                    checkComplete()
                }

                override fun onFailure(call: Call<FollowedByResponse>, t: Throwable) {
                    Log.e("FollowersFragment", "Error loading blocked", t)
                    checkComplete()
                }
            })
    }

    private fun renderAllPages(data: FollowListResponse) {
        Log.d("FollowersFragment", "Rendering all pages - pending: ${data.pending.size}, followers: ${data.followers.size}, following: ${data.following.size}, blocked: ${data.blocked.size}")

        renderPageData(FollowerListType.PENDING, data)
        renderPageData(FollowerListType.FOLLOWERS, data)
        renderPageData(FollowerListType.FOLLOWING, data)
        renderPageData(FollowerListType.BLOCKED, data)
    }

    private fun renderPageData(type: FollowerListType, data: FollowListResponse) {
        val container = pageContainers[type] ?: return
        container.removeAllViews()

        val relations = when (type) {
            FollowerListType.PENDING -> data.pending
            FollowerListType.FOLLOWERS -> data.followers
            FollowerListType.FOLLOWING -> data.following
            FollowerListType.BLOCKED -> data.blocked
        }

        val validRelations = relations.filter { it.user != null }

        if (validRelations.isNotEmpty()) {
            validRelations.forEach { relation ->
                val user = relation.user ?: return@forEach
                val followId = relation.documentId ?: relation.id.toString()
                val itemView = createFollowerItem(
                    user = user,
                    type = type,
                    followId = followId,
                    userDocumentId = user.documentId
                )
                container.addView(itemView)
            }
        } else {
            val emptyView = TextView(requireContext())
            emptyView.text = "Keine Einträge"
            emptyView.textSize = 14f
            emptyView.setPadding(16, 16, 16, 16)
            container.addView(emptyView)
        }
    }

    private fun createFollowerItem(
        user: FollowerUser,
        type: FollowerListType,
        followId: String,
        userDocumentId: String? = null
    ): View {
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.list_item_follower, null, false)

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
            FollowerListType.PENDING -> {
                btnAccept.visibility = View.VISIBLE
                btnDecline.visibility = View.VISIBLE
                btnAccept.text = "Akzeptieren"
                btnDecline.text = "Ablehnen"

                btnAccept.setOnClickListener {
                    manageFollow(followId, "accepted")
                }
                btnDecline.setOnClickListener {
                    manageFollow(followId, "declined")
                }
            }
            FollowerListType.FOLLOWERS -> {
                btnDecline.visibility = View.VISIBLE
                btnDecline.text = "Entfernen"

                btnDecline.setOnClickListener {
                    manageFollow(followId, "declined")
                }
            }
            FollowerListType.FOLLOWING -> {
                btnUnfollow.visibility = View.VISIBLE

                btnUnfollow.setOnClickListener {
                    if (userDocumentId != null) {
                        toggleFollow(userDocumentId)
                    }
                }
            }
            FollowerListType.BLOCKED -> {
                btnAccept.visibility = View.VISIBLE
                btnAccept.text = "Akzeptieren"

                btnAccept.setOnClickListener {
                    manageFollow(followId, "accepted")
                }
            }
        }

        return itemView
    }

    private fun manageFollow(followId: String, action: String) {
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
                            requireContext(),
                            "Fehler bei der Aktion",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FollowManageResponse>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "Netzwerkfehler",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FollowersFragment", "Error managing follow", t)
                }
            })
    }

    private fun toggleFollow(userDocumentId: String) {
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
                            requireContext(),
                            "Fehler beim Entfolgen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FollowToggleResponse>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "Netzwerkfehler",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FollowersFragment", "Error toggling follow", t)
                }
            })
    }
}
