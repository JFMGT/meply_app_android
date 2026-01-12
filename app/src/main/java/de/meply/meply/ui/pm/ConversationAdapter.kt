package de.meply.meply.ui.pm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.meply.meply.R
import de.meply.meply.data.messages.Conversation
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val conversations: MutableList<Conversation>,
    private val onConversationClick: (Conversation) -> Unit,
    private val currentUserId: String?,
    private val imageBaseUrl: String = "https://admin.meeplemates.de"
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.conversationAvatar)
        val partnerName: TextView = view.findViewById(R.id.conversationPartnerName)
        val lastMessageTime: TextView = view.findViewById(R.id.conversationLastMessageTime)
        val unreadIndicator: View = view.findViewById(R.id.conversationUnreadIndicator)
        val meetingInfo: LinearLayout = view.findViewById(R.id.conversationMeetingInfo)
        val meetingTitle: TextView = view.findViewById(R.id.conversationMeetingTitle)
        val meetingEvent: TextView = view.findViewById(R.id.conversationMeetingEvent)
        val meetingLocation: TextView = view.findViewById(R.id.conversationMeetingLocation)
        val meetingDate: TextView = view.findViewById(R.id.conversationMeetingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        val context = holder.itemView.context

        // Find the other participant (not the current user)
        val partner = conversation.participants.firstOrNull {
            it.documentId != currentUserId
        } ?: conversation.participants.firstOrNull()

        // Partner name
        holder.partnerName.text = partner?.username ?: "Unbekannt"

        // Avatar
        val avatarUrl = partner?.avatar?.firstOrNull()?.url
        if (avatarUrl != null) {
            Glide.with(context)
                .load(imageBaseUrl + avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.avatar)
        } else {
            // Generate fallback avatar based on user ID
            val hash = partner?.id?.hashCode() ?: 0
            val avatarIndex = (Math.abs(hash) % 8) + 1
            holder.avatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Last message time
        val relativeTime = formatRelativeTime(conversation.lastMessageAt)
        holder.lastMessageTime.text = relativeTime

        // Unread indicator
        holder.unreadIndicator.visibility = if (conversation.hasUnread) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Meeting info
        if (conversation.meeting != null) {
            holder.meetingInfo.visibility = View.VISIBLE
            holder.meetingTitle.text = conversation.meeting.title ?: "Meeting"

            val eventText = conversation.meeting.event?.title
            if (eventText != null) {
                holder.meetingEvent.text = "Event: $eventText"
                holder.meetingEvent.visibility = View.VISIBLE
            } else {
                holder.meetingEvent.visibility = View.GONE
            }

            val locationText = conversation.meeting.location?.titel
            if (locationText != null) {
                holder.meetingLocation.text = "Ort: $locationText"
                holder.meetingLocation.visibility = View.VISIBLE
            } else {
                holder.meetingLocation.visibility = View.GONE
            }

            val dateText = formatDate(conversation.meeting.date)
            if (dateText != null) {
                holder.meetingDate.text = "Datum: $dateText"
                holder.meetingDate.visibility = View.VISIBLE
            } else {
                holder.meetingDate.visibility = View.GONE
            }
        } else {
            holder.meetingInfo.visibility = View.GONE
        }

        // Click listener
        holder.itemView.setOnClickListener {
            onConversationClick(conversation)
        }
    }

    override fun getItemCount(): Int = conversations.size

    fun updateConversations(newConversations: List<Conversation>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
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
                minutes < 60 -> "vor $minutes Min."
                hours < 24 -> "vor $hours Std."
                days < 7 -> "vor $days Tage"
                else -> {
                    val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    displayFormat.format(date)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatDate(dateStr: String?): String? {
        if (dateStr.isNullOrEmpty()) return null

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateStr) ?: return null

            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
