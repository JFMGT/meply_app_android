package de.meply.meply.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.events.EventAttributes
import de.meply.meply.data.events.EventItem
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter(
    private val onClick: (EventItem) -> Unit,
    private val onLikeClick: ((EventItem, Int) -> Unit)? = null
) : RecyclerView.Adapter<EventsAdapter.VH>() {

    private val items = mutableListOf<EventItem>()

    fun submit(list: List<EventItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun updateLikeCount(position: Int, newCount: Int, isLiked: Boolean) {
        if (position in items.indices) {
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(item.attributes)
        holder.itemView.setOnClickListener { onClick(item) }

        // Like click handler
        holder.likesContainer.setOnClickListener {
            onLikeClick?.invoke(item, position)
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val title = v.findViewById<TextView>(R.id.title)
        private val meta = v.findViewById<TextView>(R.id.meta)
        private val desc = v.findViewById<TextView>(R.id.desc)
        private val likeIcon = v.findViewById<ImageView>(R.id.likeIcon)
        private val likeCount = v.findViewById<TextView>(R.id.likeCount)
        private val meetingCount = v.findViewById<TextView>(R.id.meetingCount)
        val likesContainer: LinearLayout = v.findViewById(R.id.likesContainer)

        private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val de = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

        fun bind(a: EventAttributes) {
            title.text = a.title ?: "Ohne Titel"
            desc.text = a.description ?: ""

            val start = a.startDate?.let { runCatching { iso.parse(it) }.getOrNull() }
            val end = a.endDate?.let { runCatching { iso.parse(it) }.getOrNull() }

            val dateText = when {
                start != null && end != null -> "${de.format(start)} bis ${de.format(end)}"
                start != null -> de.format(start)
                else -> "Datum folgt"
            }
            val city = a.city?.trim().orEmpty().ifEmpty { "Unbekannter Ort" }
            meta.text = "$city • $dateText"

            // Likes
            val likes = a.likes ?: 0
            likeCount.text = likes.toString()

            // Like icon based on liked state - set both drawable and tint
            // Use white for liked (visible on yellow footer) and dark for not liked
            if (a.liked) {
                likeIcon.setImageResource(R.drawable.ic_star_filled)
                likeIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.white))
            } else {
                likeIcon.setImageResource(R.drawable.ic_star_outline)
                likeIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.text_on_light))
            }

            // Meeting count
            val meetings = a.meetingCount ?: 0
            meetingCount.text = if (meetings == 1) "1 Gesuch" else "$meetings Gesuche"
        }
    }
}
