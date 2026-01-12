package de.meply.meply.ui.pm

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import de.meply.meply.R
import de.meply.meply.data.messages.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String?
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
        val card: MaterialCardView = view.findViewById(R.id.messageCard)
        val author: TextView = view.findViewById(R.id.messageAuthor)
        val content: TextView = view.findViewById(R.id.messageContent)
        val time: TextView = view.findViewById(R.id.messageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isOwnMessage = message.author.documentId == currentUserId

        // Author name
        holder.author.text = message.author.username

        // Content
        if (message.deletedByUser) {
            holder.content.text = "[Diese Nachricht wurde gelöscht]"
            holder.content.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        } else {
            holder.content.text = message.content
            holder.content.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }

        // Time
        val relativeTime = formatRelativeTime(message.createdAt)
        holder.time.text = relativeTime

        // Alignment and styling based on sender
        val layoutParams = holder.card.layoutParams as LinearLayout.LayoutParams
        if (isOwnMessage) {
            // Own messages: aligned to the right
            holder.container.gravity = Gravity.END
            layoutParams.marginStart = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_opposite)
            layoutParams.marginEnd = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_own)
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.purple_200))
        } else {
            // Partner messages: aligned to the left
            holder.container.gravity = Gravity.START
            layoutParams.marginStart = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_own)
            layoutParams.marginEnd = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_opposite)
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
        }
        holder.card.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
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
                    val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    displayFormat.format(date)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }
}
