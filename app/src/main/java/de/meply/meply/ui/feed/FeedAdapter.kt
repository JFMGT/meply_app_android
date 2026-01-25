package de.meply.meply.ui.feed

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import de.meply.meply.R
import de.meply.meply.data.feed.Post
import de.meply.meply.network.ApiClient
import de.meply.meply.utils.AvatarUtils
import java.text.SimpleDateFormat
import java.util.*

class FeedAdapter(
    private val posts: MutableList<Post>,
    private val onLikeClick: (Post) -> Unit,
    private val onReplyClick: (Post) -> Unit,
    private val onShowRepliesClick: (Post) -> Unit,
    private val onOptionsClick: (Post, View) -> Unit,
    private val onImageClick: (List<String>, Int) -> Unit,
    private val onAuthorClick: ((String) -> Unit)? = null,
    private val imageBaseUrl: String = ApiClient.STRAPI_IMAGE_BASE,
    private var currentUserId: String? = null
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
    }

    fun isOwnPost(position: Int): Boolean {
        if (position < 0 || position >= posts.size) return false
        val post = posts[position]
        return currentUserId != null && post.author?.documentId == currentUserId
    }

    fun getPostAt(position: Int): Post? {
        return if (position >= 0 && position < posts.size) posts[position] else null
    }

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.postAvatar)
        val username: TextView = view.findViewById(R.id.postUsername)
        val meta: TextView = view.findViewById(R.id.postMeta)
        val content: TextView = view.findViewById(R.id.postContent)
        val imageViewPager: ViewPager2 = view.findViewById(R.id.postImageViewPager)
        val imageCounter: TextView = view.findViewById(R.id.postImageCounter)
        val likeButton: ImageButton = view.findViewById(R.id.postLikeButton)
        val likeCount: TextView = view.findViewById(R.id.postLikeCount)
        val replyButton: ImageButton = view.findViewById(R.id.postReplyButton)
        val replyCount: TextView = view.findViewById(R.id.postReplyCount)
        val showRepliesLink: TextView = view.findViewById(R.id.postShowRepliesLink)
        val optionsButton: ImageButton = view.findViewById(R.id.postOptionsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val context = holder.itemView.context

        Log.d("FeedAdapter", "onBindViewHolder position=$position, documentId=${post.documentId}, likeCount=${post.likeCount}, liked=${post.liked}, isDeleted=${post.isDeleted}")

        // Check if post is deleted
        val isDeleted = post.isDeleted
        val author = post.author

        // Username - show "Gelöschter Beitrag" for deleted posts
        if (isDeleted) {
            holder.username.text = "Gelöschter Beitrag"
            holder.username.setTextColor(context.getColor(R.color.text_secondary))
            holder.username.setOnClickListener(null)
        } else {
            holder.username.text = author?.username ?: "Unbekannt"

            // Make username clickable if callback is provided
            val authorUserslug = author?.userslug
            if (onAuthorClick != null && !authorUserslug.isNullOrEmpty()) {
                holder.username.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                holder.username.setOnClickListener {
                    onAuthorClick.invoke(authorUserslug)
                }
            } else {
                // Reset to default if no callback
                holder.username.setTextColor(context.getColor(R.color.text_on_light))
                holder.username.setOnClickListener(null)
            }
        }

        // Avatar - show placeholder for deleted posts
        if (isDeleted) {
            holder.avatar.setImageResource(R.drawable.ic_launcher_foreground)
            holder.avatar.alpha = 0.5f
        } else {
            holder.avatar.alpha = 1.0f
            val avatarUrl = author?.avatar?.firstOrNull()?.formats?.thumbnail?.url
            if (avatarUrl != null) {
                // User has uploaded avatar
                Glide.with(context)
                    .load(imageBaseUrl + avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.avatar)
            } else {
                // Generate default avatar based on userId (matching PHP implementation)
                val userId = author?.userId ?: author?.documentId ?: "default"
                val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(userId)
                Glide.with(context)
                    .load(defaultAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(holder.avatar)
            }
        }

        // Meta: timestamp + visibility (or deletion notice)
        if (isDeleted) {
            holder.meta.text = "Beitrag gelöscht"
        } else {
            val relativeTime = formatRelativeTime(post.createdAt)
            val visibilityText = when (post.visibility) {
                "public" -> "öffentlich"
                "members" -> "Mitglieder"
                "follower" -> "Follower"
                else -> post.visibility
            }
            holder.meta.text = "$relativeTime • $visibilityText"
        }

        // Content - show placeholder for deleted posts
        if (isDeleted) {
            holder.content.text = "[Dieser Beitrag wurde gelöscht]"
            holder.content.setTextColor(context.getColor(R.color.text_secondary))
        } else {
            holder.content.text = post.content ?: ""
            holder.content.setTextColor(context.getColor(R.color.text_on_light))
        }

        // Images - hide for deleted posts
        if (isDeleted || post.image.isNullOrEmpty()) {
            holder.imageViewPager.visibility = View.GONE
            holder.imageCounter.visibility = View.GONE
        } else {
            holder.imageViewPager.visibility = View.VISIBLE
            holder.imageCounter.visibility = View.VISIBLE

            val imageUrls = post.image.mapNotNull { img ->
                val format = img.formats?.medium?.url ?: img.formats?.small?.url ?: img.url
                if (format.isNotEmpty()) imageBaseUrl + format else null
            }

            val imageAdapter = PostImageAdapter(imageUrls) { images, position ->
                onImageClick(images, position)
            }
            holder.imageViewPager.adapter = imageAdapter

            // Update counter when page changes
            holder.imageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    holder.imageCounter.text = "${position + 1} / ${imageUrls.size}"
                }
            })

            // Initial counter
            holder.imageCounter.text = "1 / ${imageUrls.size}"
        }

        // Like button - disable for deleted posts
        val likeIcon = if (post.liked) {
            R.drawable.ic_star_filled
        } else {
            R.drawable.ic_star_outline
        }
        holder.likeButton.setImageResource(likeIcon)
        holder.likeCount.text = post.likeCount.toString()
        if (isDeleted) {
            holder.likeButton.isEnabled = false
            holder.likeButton.alpha = 0.5f
            holder.likeButton.setOnClickListener(null)
        } else {
            holder.likeButton.isEnabled = true
            holder.likeButton.alpha = 1.0f
            holder.likeButton.setOnClickListener {
                onLikeClick(post)
            }
        }

        // Reply button - disable for deleted posts
        holder.replyCount.text = post.replyCount.toString()
        if (isDeleted) {
            holder.replyButton.isEnabled = false
            holder.replyButton.alpha = 0.5f
            holder.replyButton.setOnClickListener(null)
        } else {
            holder.replyButton.isEnabled = true
            holder.replyButton.alpha = 1.0f
            holder.replyButton.setOnClickListener {
                onReplyClick(post)
            }
        }

        // Show replies link - still show for deleted posts if there are replies
        if (post.replyCount > 0) {
            holder.showRepliesLink.visibility = View.VISIBLE
            val repliesText = if (post.replyCount == 1) {
                "Antwort anzeigen"
            } else {
                "${post.replyCount} Antworten anzeigen"
            }
            holder.showRepliesLink.text = repliesText
            holder.showRepliesLink.setOnClickListener {
                onShowRepliesClick(post)
            }
        } else {
            holder.showRepliesLink.visibility = View.GONE
        }

        // Options button - hide for deleted posts
        if (isDeleted) {
            holder.optionsButton.visibility = View.GONE
        } else {
            holder.optionsButton.visibility = View.VISIBLE
            holder.optionsButton.setOnClickListener {
                onOptionsClick(post, it)
            }
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun addPosts(newPosts: List<Post>) {
        val startPosition = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(startPosition, newPosts.size)
    }

    fun updatePost(updatedPost: Post) {
        val index = posts.indexOfFirst { it.documentId == updatedPost.documentId }
        Log.d("FeedAdapter", "updatePost called: documentId=${updatedPost.documentId}, index=$index, newLikeCount=${updatedPost.likeCount}, liked=${updatedPost.liked}")
        if (index != -1) {
            posts[index] = updatedPost
            Log.d("FeedAdapter", "Calling notifyItemChanged($index) with payload")
            notifyItemChanged(index, PAYLOAD_LIKE_UPDATE)
        } else {
            Log.w("FeedAdapter", "Post not found in list: ${updatedPost.documentId}")
        }
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_LIKE_UPDATE)) {
            // Partial update - only update like related views
            val post = posts[position]
            Log.d("FeedAdapter", "Partial bind (like update) position=$position, likeCount=${post.likeCount}, liked=${post.liked}")
            val likeIcon = if (post.liked) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            holder.likeButton.setImageResource(likeIcon)
            holder.likeCount.text = post.likeCount.toString()
        } else {
            // Full bind
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    companion object {
        private const val PAYLOAD_LIKE_UPDATE = "like_update"
    }

    fun removePost(documentId: String) {
        val index = posts.indexOfFirst { it.documentId == documentId }
        if (index != -1) {
            posts.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    /**
     * Format timestamp as relative time (e.g., "2 hours ago")
     */
    private fun formatRelativeTime(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return ""

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(timestamp) ?: return ""

            val now = System.currentTimeMillis()
            val diff = now - date.time

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 60 -> "gerade eben"
                minutes < 60 -> "$minutes Min."
                hours < 24 -> "$hours Std."
                days < 7 -> "$days Tage"
                else -> {
                    val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    displayFormat.format(date)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }
}
