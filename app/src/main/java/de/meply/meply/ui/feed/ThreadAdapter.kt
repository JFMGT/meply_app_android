package de.meply.meply.ui.feed

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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for thread view with nested replies
 * Each post includes its nesting level for indentation
 */
class ThreadAdapter(
    private val posts: MutableList<PostWithLevel>,
    private val onLikeClick: (Post) -> Unit,
    private val onReplyClick: (Post) -> Unit,
    private val onOptionsClick: (Post, View) -> Unit,
    private val onImageClick: (List<String>, Int) -> Unit,
    private val imageBaseUrl: String = "https://admin.meeplemates.de"
) : RecyclerView.Adapter<ThreadAdapter.PostViewHolder>() {

    data class PostWithLevel(
        val post: Post,
        val level: Int
    )

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
        val postWithLevel = posts[position]
        val post = postWithLevel.post
        val level = postWithLevel.level
        val context = holder.itemView.context

        // Apply indentation based on nesting level
        val indentDp = level * 16 // 16dp per level
        val indentPx = (indentDp * context.resources.displayMetrics.density).toInt()
        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = indentPx
        holder.itemView.layoutParams = layoutParams

        // Author kann null sein wenn z.B. der Elternbeitrag gelöscht wurde
        val author = post.author

        // Username
        holder.username.text = author?.username ?: "Unbekannt"

        // Avatar
        val avatarUrl = author?.avatar?.firstOrNull()?.formats?.thumbnail?.url
        if (avatarUrl != null) {
            Glide.with(context)
                .load(imageBaseUrl + avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.avatar)
        } else {
            val hash = author?.id?.hashCode() ?: 0
            val avatarIndex = (Math.abs(hash) % 8) + 1
            holder.avatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Meta
        val relativeTime = formatRelativeTime(post.createdAt)
        val visibilityText = when (post.visibility) {
            "public" -> "öffentlich"
            "members" -> "Mitglieder"
            "follower" -> "Follower"
            else -> post.visibility
        }
        holder.meta.text = "$relativeTime • $visibilityText"

        // Content
        holder.content.text = post.content ?: ""

        // Images
        if (!post.image.isNullOrEmpty()) {
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

            holder.imageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    holder.imageCounter.text = "${position + 1} / ${imageUrls.size}"
                }
            })

            holder.imageCounter.text = "1 / ${imageUrls.size}"
        } else {
            holder.imageViewPager.visibility = View.GONE
            holder.imageCounter.visibility = View.GONE
        }

        // Like button
        val likeIcon = if (post.liked) {
            R.drawable.ic_star_filled
        } else {
            R.drawable.ic_star_outline
        }
        holder.likeButton.setImageResource(likeIcon)
        holder.likeCount.text = post.likeCount.toString()
        holder.likeButton.setOnClickListener {
            onLikeClick(post)
        }

        // Reply button
        holder.replyCount.text = post.replyCount.toString()
        holder.replyButton.setOnClickListener {
            onReplyClick(post)
        }

        // Hide "Show replies" link in thread view (all replies are already shown)
        holder.showRepliesLink.visibility = View.GONE

        // Options button
        holder.optionsButton.setOnClickListener {
            onOptionsClick(post, it)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updateThread(rootPost: Post) {
        posts.clear()
        flattenPostTree(rootPost, 0)
        notifyDataSetChanged()
    }

    /**
     * Flatten the nested post tree into a flat list with levels
     */
    private fun flattenPostTree(post: Post, level: Int) {
        posts.add(PostWithLevel(post, level))
        post.children?.forEach { child ->
            flattenPostTree(child, level + 1)
        }
    }

    fun updatePost(updatedPost: Post) {
        val index = posts.indexOfFirst { it.post.documentId == updatedPost.documentId }
        if (index != -1) {
            posts[index] = posts[index].copy(post = updatedPost)
            notifyItemChanged(index)
        }
    }

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
