package de.meply.meply.ui.feed

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.feed.*
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadMoreProgressBar: ProgressBar
    private lateinit var feedAdapter: FeedAdapter

    private val posts = mutableListOf<Post>()
    private var currentCursor: String? = null
    private var hasMore = true
    private var isLoading = false

    private val createPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh feed when a new post is created
            loadFeed(reset = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        recyclerView = view.findViewById(R.id.feedRecyclerView)
        swipeRefresh = view.findViewById(R.id.feedSwipeRefresh)
        progressBar = view.findViewById(R.id.feedProgressBar)
        loadMoreProgressBar = view.findViewById(R.id.feedLoadMoreProgressBar)

        setupRecyclerView()
        setupSwipeRefresh()

        // Initial load
        loadFeed(reset = true)

        return view
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            posts = posts,
            onLikeClick = { post -> toggleLike(post) },
            onReplyClick = { post -> showReplyDialog(post) },
            onShowRepliesClick = { post -> showThread(post) },
            onOptionsClick = { post, view -> showOptionsMenu(post, view) },
            onImageClick = { images, position -> showImageGallery(images, position) },
            onAuthorClick = { userSlug -> openUserProfile(userSlug) }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = feedAdapter

        // Infinite scroll
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                Log.d("FeedScroll", "Scroll detected: visible=$visibleItemCount, total=$totalItemCount, first=$firstVisibleItemPosition, last=$lastVisibleItemPosition, isLoading=$isLoading, hasMore=$hasMore")

                if (!isLoading && hasMore && dy > 0) {
                    if ((lastVisibleItemPosition + 3) >= totalItemCount
                        && totalItemCount > 0
                    ) {
                        Log.d("FeedScroll", "Triggering load more. Cursor: $currentCursor")
                        loadFeed(reset = false)
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadFeed(reset = true)
        }
    }
    /**
     * Public method to refresh the feed from outside (e.g., HomeActivity)
     */
    fun refreshFeed() {
        loadFeed(reset = true)
    }


    private fun loadFeed(reset: Boolean) {
        if (isLoading) {
            Log.d("FeedFragment", "Already loading, skipping")
            return
        }
        isLoading = true

        if (reset) {
            currentCursor = null
            hasMore = true
            progressBar.visibility = View.VISIBLE
            loadMoreProgressBar.visibility = View.GONE
            Log.d("FeedFragment", "Loading feed (reset)")
        } else {
            // Loading more posts
            loadMoreProgressBar.visibility = View.VISIBLE
            Log.d("FeedFragment", "Loading more posts with cursor: $currentCursor")
        }

        val api = ApiClient.retrofit
        val call = api.getFeed(
            limit = 10,
            before = if (reset) null else currentCursor
        )
        Log.d("FeedFragment", "API call created with before=${if (reset) "null" else currentCursor}")

        call.enqueue(object : Callback<FeedResponse> {
            override fun onResponse(call: Call<FeedResponse>, response: Response<FeedResponse>) {
                isLoading = false
                progressBar.visibility = View.GONE
                loadMoreProgressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val feedResponse = response.body()
                    if (feedResponse != null) {
                        Log.d("FeedFragment", "Response received: ${feedResponse.feed.size} posts, hasMore=${feedResponse.hasMore}, cursor.hasMore=${feedResponse.cursor?.hasMore}, cursor=${feedResponse.cursor?.oldestCreatedAt}")

                        if (reset) {
                            feedAdapter.updatePosts(feedResponse.feed)
                        } else {
                            feedAdapter.addPosts(feedResponse.feed)
                        }

                        currentCursor = feedResponse.cursor?.oldestCreatedAt
                        // Use hasMore from cursor if available, otherwise from response
                        hasMore = feedResponse.cursor?.hasMore ?: feedResponse.hasMore

                        Log.d("FeedFragment", "State after load: currentCursor=$currentCursor, hasMore=$hasMore, totalPosts=${posts.size}")
                    } else {
                        Log.e("FeedFragment", "Response body is null")
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Laden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("FeedFragment", "Error loading feed: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<FeedResponse>, t: Throwable) {
                isLoading = false
                progressBar.visibility = View.GONE
                loadMoreProgressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("FeedFragment", "Network error loading feed", t)
            }
        })
    }

    private fun toggleLike(post: Post) {
        val api = ApiClient.retrofit
        val request = LikeToggleRequest(
            targetDocumentId = post.documentId,
            targetType = "post"
        )

        api.toggleLike(request).enqueue(object : Callback<LikeToggleResponse> {
            override fun onResponse(
                call: Call<LikeToggleResponse>,
                response: Response<LikeToggleResponse>
            ) {
                if (response.isSuccessful) {
                    val likeResponse = response.body()
                    if (likeResponse != null) {
                        // Update post in adapter
                        val updatedPost = post.copy(
                            liked = likeResponse.status == "liked",
                            likeCount = likeResponse.likeCount
                        )
                        feedAdapter.updatePost(updatedPost)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Liken",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LikeToggleResponse>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showReplyDialog(post: Post) {
        val intent = Intent(requireContext(), CreatePostActivity::class.java)
        intent.putExtra("parentDocumentId", post.documentId)
        createPostLauncher.launch(intent)
    }

    private fun showThread(post: Post) {
        val intent = Intent(requireContext(), ThreadActivity::class.java)
        intent.putExtra("documentId", post.documentId)
        startActivity(intent)
    }

    private fun showOptionsMenu(post: Post, anchorView: View) {
        // Check if current user is the post author
        val currentUserId = AuthManager.getProfileDocumentId(requireContext())
        val isOwnPost = currentUserId != null && currentUserId == post.author.documentId

        val options = mutableListOf<String>()

        // Show "Melden" only for posts by other users
        if (!isOwnPost) {
            options.add("Melden")
        }

        // Show "Löschen" only for own posts
        if (isOwnPost) {
            options.add("Löschen")
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Optionen")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Melden" -> showReportDialog(post)
                    "Löschen" -> confirmDelete(post)
                }
            }
            .show()
    }

    private fun showReportDialog(post: Post) {
        val input = TextInputEditText(requireContext())
        input.hint = "Grund für die Meldung (optional)"

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Post melden")
            .setMessage("Möchtest du diesen Post melden?")
            .setView(input)
            .setPositiveButton("Melden") { _, _ ->
                val reason = input.text?.toString() ?: "standard"
                reportPost(post, reason)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun reportPost(post: Post, reason: String) {
        val api = ApiClient.retrofit
        val request = ReportPostRequest(
            targetDocumentId = post.documentId,
            targetType = "post",
            reason = reason
        )

        api.reportPost(request).enqueue(object : Callback<ReportPostResponse> {
            override fun onResponse(
                call: Call<ReportPostResponse>,
                response: Response<ReportPostResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        requireContext(),
                        "Post wurde gemeldet",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Melden",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ReportPostResponse>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun confirmDelete(post: Post) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Post löschen")
            .setMessage("Möchtest du diesen Post wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun deletePost(post: Post) {
        val api = ApiClient.retrofit

        api.deletePost(post.documentId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    feedAdapter.removePost(post.documentId)
                    Toast.makeText(
                        requireContext(),
                        "Post gelöscht",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Löschen",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showImageGallery(images: List<String>, startPosition: Int) {
        // TODO: Implement image gallery view (fullscreen with swipe)
        Toast.makeText(
            requireContext(),
            "Image ${startPosition + 1} of ${images.size}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openUserProfile(userSlug: String) {
        de.meply.meply.ui.profile.UserProfileActivity.start(requireContext(), userSlug)
    }
}
