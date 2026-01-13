package de.meply.meply.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.meetings.MeetingData
import java.text.SimpleDateFormat
import java.util.Locale

class MeetingsAdapter(
    private val onContactClick: (MeetingData) -> Unit,
    private val onAuthorClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<MeetingsAdapter.MeetingViewHolder>() {

    private var meetings: List<MeetingData> = emptyList()

    fun submitList(newMeetings: List<MeetingData>) {
        meetings = newMeetings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meeting, parent, false)
        return MeetingViewHolder(view, onContactClick, onAuthorClick)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        holder.bind(meetings[position])
    }

    override fun getItemCount(): Int = meetings.size

    class MeetingViewHolder(
        itemView: View,
        private val onContactClick: (MeetingData) -> Unit,
        private val onAuthorClick: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleText: TextView = itemView.findViewById(R.id.meeting_title)
        private val dateText: TextView = itemView.findViewById(R.id.meeting_date)
        private val descriptionText: TextView = itemView.findViewById(R.id.meeting_description)
        private val authorText: TextView = itemView.findViewById(R.id.meeting_author)
        private val contactButton: Button = itemView.findViewById(R.id.meeting_contact_button)

        fun bind(meeting: MeetingData) {
            // Title
            titleText.text = meeting.title ?: "Spieltreffen"

            // Date formatting
            dateText.text = formatMeetingDate(meeting)

            // Description
            descriptionText.text = meeting.description ?: "Keine Beschreibung"

            // Author info
            val authorName = meeting.author?.username ?: "Unbekannt"
            val authorCity = meeting.author?.city ?: ""
            authorText.text = if (authorCity.isNotEmpty()) {
                "Von: $authorName • $authorCity"
            } else {
                "Von: $authorName"
            }

            // Make author name clickable if callback is provided
            val authorUserslug = meeting.author?.userSlug
            if (onAuthorClick != null && !authorUserslug.isNullOrEmpty()) {
                authorText.setTextColor(authorText.context.getColor(android.R.color.holo_blue_dark))
                authorText.setOnClickListener {
                    onAuthorClick.invoke(authorUserslug)
                }
            }

            // Contact button
            contactButton.setOnClickListener {
                onContactClick(meeting)
            }
        }

        private fun formatMeetingDate(meeting: MeetingData): String {
            // Check if dates object exists
            val dates = meeting.dates
            if (dates != null) {
                when (dates.type) {
                    "fixed" -> {
                        val dateValue = dates.value?.fixedDateValue
                        return if (!dateValue.isNullOrEmpty()) {
                            "📅 ${formatDate(dateValue)}"
                        } else {
                            "📅 Datum wird festgelegt"
                        }
                    }
                    "range" -> {
                        val start = dates.value?.rangeStartValue
                        val end = dates.value?.rangeEndValue
                        return if (!start.isNullOrEmpty() && !end.isNullOrEmpty()) {
                            "📅 ${formatDate(start)} - ${formatDate(end)}"
                        } else {
                            "📅 Zeitraum wird festgelegt"
                        }
                    }
                    "recurring" -> {
                        val frequency = dates.value?.frequency ?: "regelmäßig"
                        return "📅 $frequency"
                    }
                    "eventDays" -> {
                        return "📅 An den Event-Tagen"
                    }
                }
            }

            // Fallback to simple date field
            val simpleDate = meeting.date
            return if (!simpleDate.isNullOrEmpty()) {
                "📅 ${formatDate(simpleDate)}"
            } else {
                "📅 Datum wird festgelegt"
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    outputFormat.format(date)
                } else {
                    dateString
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }
}
