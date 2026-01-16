package de.meply.meply.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import de.meply.meply.R
import de.meply.meply.data.feed.Post
import de.meply.meply.utils.AvatarUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for thread view with Reddit-style colored depth lines.
 * Each depth level has its own color, and lines run continuously
 * to show the conversation structure clearly.
 */
class ThreadAdapter(
    private val posts: MutableList<ThreadPost>,
    private val onLikeClick: (Post) -> Unit,
    private val onReplyClick: (Post) -> Unit,
    private val onOptionsClick: (Post, View) -> Unit,
    private val onImageClick: (List<String>, Int) -> Unit,
    private val onOpenThreadClick: ((Post) -> Unit)? = null,
    private val imageBaseUrl: String = "https://admin.meeplemates.de"
) : RecyclerView.Adapter<ThreadAdapter.ThreadViewHolder>() {

    companion object {
        private const val MAX_DEPTH = 5  // Maximum supported depth for lines

        // Thread line colors (matching colors.xml)
        private val DEPTH_COLORS = intArrayOf(
            R.color.thread_line_0,  // Yellow
            R.color.thread_line_1,  // Light Blue
            R.color.thread_line_2,  // Light Green
            R.color.thread_line_3,  // Light Orange
            R.color.thread_line_4,  // Light Purple
            R.color.thread_line_5   // Cyan
        )
    }

    /**
     * Represents a post in the flattened thread list with its visual properties
     */
    data class ThreadPost(
        val post: Post,
        val depth: Int,                    // Actual depth in the tree (0 = root)
        val visibleLines: BooleanArray,    // Which depth lines should be visible
        val isFirstChild: Boolean = false, // First child of a parent (shows branch start)
        val isLastChild: Boolean = false,  // Last child at this depth (line ends here)
        val hasChildren: Boolean = false,  // Has children that will be shown
        val hasHiddenChildren: Boolean = false  // Has children but they're hidden due to depth
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ThreadPost) return false
            return post.documentId == other.post.documentId
        }

        override fun hashCode(): Int = post.documentId.hashCode()
    }

    inner class ThreadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        // Depth lines
        val depthLines: Array<View?> = arrayOf(
            view.findViewById(R.id.depthLine0),
            view.findViewById(R.id.depthLine1),
            view.findViewById(R.id.depthLine2),
            view.findViewById(R.id.depthLine3),
            view.findViewById(R.id.depthLine4)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thread_post, parent, false)
        return ThreadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        val threadPost = posts[position]
        val post = threadPost.post
        val context = holder.itemView.context

        // Setup depth lines visibility
        for (i in 0 until MAX_DEPTH) {
            val line = holder.depthLines.getOrNull(i)
            if (line != null) {
                if (i < threadPost.visibleLines.size && threadPost.visibleLines[i]) {
                    line.visibility = View.VISIBLE
                    line.setBackgroundColor(ContextCompat.getColor(context, DEPTH_COLORS[i % DEPTH_COLORS.size]))
                } else {
                    line.visibility = View.GONE
                }
            }
        }

        // Author info
        val author = post.author
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
            val userId = author?.userId ?: author?.documentId ?: "default"
            val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(userId)
            Glide.with(context)
                .load(defaultAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.avatar)
        }

        // Meta info
        val relativeTime = formatRelativeTime(post.createdAt)
        holder.meta.text = relativeTime

        // Content
        holder.content.text = post.content ?: ""

        // Images
        if (!post.image.isNullOrEmpty()) {
            holder.imageViewPager.visibility = View.VISIBLE
            holder.imageCounter.visibility = if (post.image.size > 1) View.VISIBLE else View.GONE

            val imageUrls = post.image.mapNotNull { img ->
                val format = img.formats?.medium?.url ?: img.formats?.small?.url ?: img.url
                if (format.isNotEmpty()) imageBaseUrl + format else null
            }

            val imageAdapter = PostImageAdapter(imageUrls) { images, pos ->
                onImageClick(images, pos)
            }
            holder.imageViewPager.adapter = imageAdapter

            holder.imageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    holder.imageCounter.text = "${pos + 1} / ${imageUrls.size}"
                }
            })

            holder.imageCounter.text = "1 / ${imageUrls.size}"
        } else {
            holder.imageViewPager.visibility = View.GONE
            holder.imageCounter.visibility = View.GONE
        }

        // Like button
        val likeIcon = if (post.liked) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        holder.likeButton.setImageResource(likeIcon)
        holder.likeCount.text = post.likeCount.toString()
        holder.likeButton.setOnClickListener { onLikeClick(post) }

        // Reply button
        holder.replyCount.text = post.replyCount.toString()
        holder.replyButton.setOnClickListener { onReplyClick(post) }

        // Show deeper replies link
        if (threadPost.hasHiddenChildren && onOpenThreadClick != null) {
            holder.showRepliesLink.visibility = View.VISIBLE
            holder.showRepliesLink.text = "Weitere Antworten anzeigen"
            holder.showRepliesLink.setOnClickListener {
                onOpenThreadClick.invoke(post)
            }
        } else {
            holder.showRepliesLink.visibility = View.GONE
        }

        // Options button
        holder.optionsButton.setOnClickListener { onOptionsClick(post, it) }
    }

    override fun getItemCount(): Int = posts.size

    /**
     * Update the thread with a new root post
     */
    fun updateThread(rootPost: Post) {
        posts.clear()

        // First pass: flatten the tree and collect depth info
        val flatList = mutableListOf<Pair<Post, Int>>()  // Post + depth
        flattenTree(rootPost, 0, flatList)

        // Second pass: calculate which lines should be visible for each post
        for (i in flatList.indices) {
            val (post, depth) = flatList[i]
            val children = post.children ?: emptyList()
            val hasChildren = children.isNotEmpty()
            val hasHiddenChildren = depth >= MAX_DEPTH && hasChildren

            // Calculate visible lines: a line at depth D is visible if there's
            // a post at depth <= D later in the list (the branch continues)
            val visibleLines = BooleanArray(MAX_DEPTH) { false }

            for (d in 0 until minOf(depth, MAX_DEPTH)) {
                // Check if any following post has depth <= d (branch continues at that level)
                for (j in (i + 1) until flatList.size) {
                    if (flatList[j].second <= d) {
                        // Found a post that returns to this level or higher
                        visibleLines[d] = true
                        break
                    }
                    if (flatList[j].second > d) {
                        // Still deeper, line continues
                        visibleLines[d] = true
                    }
                }
            }

            // Also show the line for current depth if there are more posts at same or deeper level
            if (depth > 0 && depth <= MAX_DEPTH) {
                val currentDepthLine = depth - 1
                for (j in (i + 1) until flatList.size) {
                    val nextDepth = flatList[j].second
                    if (nextDepth < depth) {
                        // We've gone back up, stop the line
                        break
                    }
                    if (nextDepth >= depth) {
                        // Same or deeper level continues
                        visibleLines[currentDepthLine] = true
                        break
                    }
                }
            }

            posts.add(ThreadPost(
                post = post,
                depth = depth,
                visibleLines = visibleLines,
                isFirstChild = i > 0 && flatList[i - 1].second < depth,
                isLastChild = i == flatList.lastIndex || flatList.getOrNull(i + 1)?.second?.let { it < depth } ?: true,
                hasChildren = hasChildren && depth < MAX_DEPTH,
                hasHiddenChildren = hasHiddenChildren
            ))
        }

        notifyDataSetChanged()
    }

    /**
     * Flatten the post tree into a list with depth information
     */
    private fun flattenTree(post: Post, depth: Int, result: MutableList<Pair<Post, Int>>) {
        result.add(Pair(post, depth))

        if (depth < MAX_DEPTH) {
            post.children?.forEach { child ->
                flattenTree(child, depth + 1, result)
            }
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
