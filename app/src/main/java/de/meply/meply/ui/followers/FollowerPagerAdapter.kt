package de.meply.meply.ui.followers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R

class FollowerPagerAdapter : RecyclerView.Adapter<FollowerPagerAdapter.PageViewHolder>() {

    private val pages = listOf(
        PageData("🔔 Offene Follow-Anfragen", FollowerListType.PENDING),
        PageData("🧍 Menschen die dir folgen", FollowerListType.FOLLOWERS),
        PageData("🙋 Menschen denen du folgst", FollowerListType.FOLLOWING),
        PageData("🚫 Blockierte Anfragen", FollowerListType.BLOCKED)
    )

    var onPageCreated: ((View, FollowerListType) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_follower_list, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.bind(page)
        onPageCreated?.invoke(holder.itemView, page.type)
    }

    override fun getItemCount() = pages.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        val itemsContainer: LinearLayout = itemView.findViewById(R.id.itemsContainer)

        fun bind(page: PageData) {
            sectionTitle.text = page.title
        }
    }

    data class PageData(
        val title: String,
        val type: FollowerListType
    )
}

enum class FollowerListType {
    PENDING,
    FOLLOWERS,
    FOLLOWING,
    BLOCKED
}
