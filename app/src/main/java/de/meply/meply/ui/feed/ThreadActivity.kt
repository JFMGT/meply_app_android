package de.meply.meply.ui.feed

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.BaseDetailActivity
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.feed.*
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ThreadActivity : BaseDetailActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var threadAdapter: ThreadAdapter

    private var documentId: String? = null
    private var rootPost: Post? = null

    // Track posts that were modified (liked/unliked) to sync back to parent
    private val modifiedPosts = mutableMapOf<String, Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thread)

        documentId = intent.getStringExtra("documentId")
        if (documentId == null) {
            Toast.makeText(this, "Fehler: Post-ID fehlt", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        setupSwipeRefresh()

        loadThread()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.threadRecyclerView)
        swipeRefresh = findViewById(R.id.threadSwipeRefresh)
        progressBar = findViewById(R.id.threadProgressBar)

        // Setup toolbar with back button and user menu
        setupDetailToolbar()
    }

    private fun setupRecyclerView() {
        threadAdapter = ThreadAdapter(
            posts = mutableListOf(),
            onLikeClick = { post -> toggleLike(post) },
            onReplyClick = { post -> showReplyDialog(post) },
            onOptionsClick = { post, view -> showOptionsMenu(post, view) },
            onImageClick = { images, position -> showImageGallery(images, position) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = threadAdapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadThread()
        }
    }

    private fun loadThread() {
        progressBar.visibility = View.VISIBLE

        val api = ApiClient.retrofit
        api.getPostThread(documentId!!).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val post = response.body()
                    if (post != null) {
                        rootPost = post
                        // Debug logging to see like data
                        Log.d("ThreadActivity", "Root post: documentId=${post.documentId}, liked=${post.liked}, likeCount=${post.likeCount}")
                        post.children?.forEachIndexed { index, child ->
                            Log.d("ThreadActivity", "Child $index: documentId=${child.documentId}, liked=${child.liked}, likeCount=${child.likeCount}")
                        }
                        threadAdapter.updateThread(post)
                        Log.d("ThreadActivity", "Thread loaded successfully")
                    }
                } else {
                    Toast.makeText(
                        this@ThreadActivity,
                        "Fehler beim Laden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ThreadActivity", "Error loading thread: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                Toast.makeText(
                    this@ThreadActivity,
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ThreadActivity", "Network error loading thread", t)
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
                        val isLiked = likeResponse.status == "liked"

                        // API doesn't return likeCount, so calculate it ourselves
                        val newLikeCount = if (likeResponse.hasLikeCount()) {
                            likeResponse.getActualLikeCount()
                        } else {
                            when {
                                isLiked && !post.liked -> post.likeCount + 1
                                !isLiked && post.liked -> post.likeCount - 1
                                else -> post.likeCount
                            }
                        }

                        val updatedPost = post.copy(
                            liked = isLiked,
                            likeCount = newLikeCount
                        )
                        threadAdapter.updatePost(updatedPost)

                        // Track modified post to sync back to Feed
                        modifiedPosts[updatedPost.documentId] = updatedPost
                        Log.d("ThreadActivity", "Tracked modified post: ${updatedPost.documentId}, liked=${updatedPost.liked}")
                    }
                } else {
                    Toast.makeText(
                        this@ThreadActivity,
                        "Fehler beim Liken",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LikeToggleResponse>, t: Throwable) {
                Toast.makeText(
                    this@ThreadActivity,
                    "Netzwerkfehler",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showReplyDialog(post: Post) {
        val username = post.author?.username ?: post.author?.userslug
        val bottomSheet = CreatePostBottomSheet.newInstance(post.documentId, username)
        bottomSheet.setOnPostCreatedListener {
            // Refresh thread when a new reply is created
            loadThread()
        }
        bottomSheet.show(supportFragmentManager, "createReply")
    }

    private fun showOptionsMenu(post: Post, anchorView: View) {
        // Check if current user is the post author
        val currentUserId = AuthManager.getProfileDocumentId(this)
        val isOwnPost = currentUserId != null && post.author != null && currentUserId == post.author.documentId

        val options = mutableListOf<String>()

        // Show "Melden" only for posts by other users
        if (!isOwnPost) {
            options.add("Melden")
        }

        // Show "Löschen" only for own posts
        if (isOwnPost) {
            options.add("Löschen")
        }

        MaterialAlertDialogBuilder(this, R.style.Theme_Meply_AlertDialog)
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
        val input = TextInputEditText(this)
        input.hint = "Grund für die Meldung (optional)"

        MaterialAlertDialogBuilder(this, R.style.Theme_Meply_AlertDialog)
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
                        this@ThreadActivity,
                        "Post wurde gemeldet",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ThreadActivity,
                        "Fehler beim Melden",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ReportPostResponse>, t: Throwable) {
                Toast.makeText(
                    this@ThreadActivity,
                    "Netzwerkfehler",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun confirmDelete(post: Post) {
        MaterialAlertDialogBuilder(this, R.style.Theme_Meply_AlertDialog)
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
                    Toast.makeText(
                        this@ThreadActivity,
                        "Post gelöscht",
                        Toast.LENGTH_SHORT
                    ).show()
                    // If root post was deleted, go back
                    if (post.documentId == documentId) {
                        finish()
                    } else {
                        loadThread() // Refresh to show updated thread
                    }
                } else {
                    Toast.makeText(
                        this@ThreadActivity,
                        "Fehler beim Löschen",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(
                    this@ThreadActivity,
                    "Netzwerkfehler",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showImageGallery(images: List<String>, startPosition: Int) {
        Toast.makeText(
            this,
            "Image ${startPosition + 1} of ${images.size}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun finish() {
        // Return modified posts to the calling activity/fragment
        if (modifiedPosts.isNotEmpty()) {
            val gson = com.google.gson.Gson()
            val modifiedPostsList = modifiedPosts.values.toList()
            val resultIntent = Intent()
            resultIntent.putExtra("modifiedPosts", gson.toJson(modifiedPostsList))
            setResult(Activity.RESULT_OK, resultIntent)
            Log.d("ThreadActivity", "Returning ${modifiedPostsList.size} modified posts")
        }
        super.finish()
    }
}
