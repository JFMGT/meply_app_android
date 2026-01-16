package de.meply.meply.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import de.meply.meply.R
import de.meply.meply.data.feed.Post
import de.meply.meply.utils.AvatarUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for thread view with tree-style connectors.
 * Shows visual tree structure with colored lines:
 * - Vertical lines show continuing branches
 * - Horizontal connectors branch to each post
 * - └ shape for last child, ├ shape for others
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
        private const val MAX_DEPTH = 5
    }

    /**
     * Represents a post with tree visualization info
     */
    data class ThreadPost(
        val post: Post,
        val depth: Int,
        // For each depth level 0..depth-1: should we show the bottom line?
        // (true if there are more siblings at that level)
        val showBottomLine: BooleanArray,
        val isLastChild: Boolean,
        val hasHiddenChildren: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ThreadPost) return false
            return post.documentId == other.post.documentId
        }
        override fun hashCode(): Int = post.documentId.hashCode()
    }

    /**
     * Holds references to tree column views
     */
    data class TreeColumn(
        val container: FrameLayout,
        val topLine: View,
        val bottomLine: View,
        val horizontalLine: View
    )

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

        val treeColumns: Array<TreeColumn> = arrayOf(
            TreeColumn(
                view.findViewById(R.id.treeColumn0),
                view.findViewById(R.id.line0Top),
                view.findViewById(R.id.line0Bottom),
                view.findViewById(R.id.line0Horizontal)
            ),
            TreeColumn(
                view.findViewById(R.id.treeColumn1),
                view.findViewById(R.id.line1Top),
                view.findViewById(R.id.line1Bottom),
                view.findViewById(R.id.line1Horizontal)
            ),
            TreeColumn(
                view.findViewById(R.id.treeColumn2),
                view.findViewById(R.id.line2Top),
                view.findViewById(R.id.line2Bottom),
                view.findViewById(R.id.line2Horizontal)
            ),
            TreeColumn(
                view.findViewById(R.id.treeColumn3),
                view.findViewById(R.id.line3Top),
                view.findViewById(R.id.line3Bottom),
                view.findViewById(R.id.line3Horizontal)
            ),
            TreeColumn(
                view.findViewById(R.id.treeColumn4),
                view.findViewById(R.id.line4Top),
                view.findViewById(R.id.line4Bottom),
                view.findViewById(R.id.line4Horizontal)
            )
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
        val depth = threadPost.depth
        val context = holder.itemView.context

        // Setup tree columns - RESET ALL first to avoid recycling artifacts
        for (i in 0 until MAX_DEPTH) {
            val column = holder.treeColumns[i]

            // Always reset all lines to hidden and zero height first
            column.topLine.visibility = View.GONE
            column.bottomLine.visibility = View.GONE
            column.horizontalLine.visibility = View.GONE

            // Reset heights immediately to prevent recycled artifacts
            column.topLine.layoutParams = column.topLine.layoutParams.apply { height = 0 }
            column.bottomLine.layoutParams = column.bottomLine.layoutParams.apply { height = 0 }

            if (i < depth) {
                // This column should be visible
                column.container.visibility = View.VISIBLE

                val isLastColumn = (i == depth - 1)
                val showBottom = if (i < threadPost.showBottomLine.size) threadPost.showBottomLine[i] else false

                // Top line always visible (connects from above)
                column.topLine.visibility = View.VISIBLE

                // Bottom line only visible if branch continues at this level
                column.bottomLine.visibility = if (showBottom) View.VISIBLE else View.GONE

                // Horizontal connector only on the branch column (last column for this post)
                column.horizontalLine.visibility = if (isLastColumn) View.VISIBLE else View.GONE

                // Set line heights after layout - use post to ensure container has been measured
                val shouldShowBottom = showBottom
                column.container.post {
                    val halfHeight = column.container.height / 2

                    column.topLine.layoutParams = column.topLine.layoutParams.apply {
                        height = halfHeight
                    }
                    column.topLine.requestLayout()

                    // Only set bottom line height if it should be visible
                    if (shouldShowBottom) {
                        column.bottomLine.layoutParams = column.bottomLine.layoutParams.apply {
                            height = halfHeight
                        }
                        column.bottomLine.requestLayout()
                    }
                }
            } else {
                // This column should be hidden
                column.container.visibility = View.GONE
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
        holder.meta.text = formatRelativeTime(post.createdAt)

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

    fun updateThread(rootPost: Post) {
        posts.clear()

        // Flatten tree with parent tracking
        data class FlatPost(
            val post: Post,
            val depth: Int,
            val parentIndex: Int,  // Index of parent in flatList, -1 for root
            val childIndex: Int,   // Which child of parent (0-based)
            val siblingCount: Int  // Total siblings (including self)
        )

        val flatList = mutableListOf<FlatPost>()

        fun flatten(post: Post, depth: Int, parentIdx: Int, childIdx: Int, siblingCount: Int) {
            val myIndex = flatList.size
            flatList.add(FlatPost(post, depth, parentIdx, childIdx, siblingCount))

            if (depth < MAX_DEPTH) {
                val children = post.children ?: emptyList()
                children.forEachIndexed { idx, child ->
                    flatten(child, depth + 1, myIndex, idx, children.size)
                }
            }
        }

        flatten(rootPost, 0, -1, 0, 1)

        // Build ThreadPost list with proper line visibility
        for (i in flatList.indices) {
            val flat = flatList[i]
            val post = flat.post
            val depth = flat.depth
            val children = post.children ?: emptyList()
            val hasHiddenChildren = depth >= MAX_DEPTH && children.isNotEmpty()
            val isLastChild = flat.childIndex == flat.siblingCount - 1

            // Calculate showBottomLine for each depth level
            // showBottomLine[d] = true if there's a SIBLING at depth d+1 coming
            // showBottomLine[d] = false if we LEAVE the branch (next relevant post is at depth <= d)
            val showBottomLine = BooleanArray(depth) { false }

            for (d in 0 until depth) {
                for (j in (i + 1) until flatList.size) {
                    val nextDepth = flatList[j].depth
                    if (nextDepth == d + 1) {
                        // Found a sibling at this level - line continues
                        showBottomLine[d] = true
                        break
                    }
                    if (nextDepth <= d) {
                        // Leaving this branch - line ends
                        showBottomLine[d] = false
                        break
                    }
                    // nextDepth > d + 1: this is a deeper nested post, keep searching
                }
            }

            posts.add(ThreadPost(
                post = post,
                depth = depth,
                showBottomLine = showBottomLine,
                isLastChild = isLastChild,
                hasHiddenChildren = hasHiddenChildren
            ))
        }

        notifyDataSetChanged()
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
