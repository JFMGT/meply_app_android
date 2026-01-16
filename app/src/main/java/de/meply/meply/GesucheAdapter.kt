package de.meply.meply

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.meply.meply.data.meeting.Meeting
import java.text.SimpleDateFormat
import java.util.*

class GesucheAdapter(
    private val onCreateMeetingClick: () -> Unit
) : ListAdapter<Meeting, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnCreateMeeting: MaterialButton = view.findViewById(R.id.btnCreateMeeting)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val metaText: TextView = view.findViewById(R.id.metaText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val locationText: TextView = view.findViewById(R.id.locationText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1 // +1 for header
    }

    fun getMeetingAt(position: Int): Meeting? {
        return if (position > 0 && position <= super.getItemCount()) {
            getItem(position - 1)
        } else null
    }

    fun isHeader(position: Int): Boolean = position == 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_gesuch_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_gesuch, parent, false)
                ItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.btnCreateMeeting.setOnClickListener { onCreateMeetingClick() }
            }
            is ItemViewHolder -> {
                val meeting = getItem(position - 1) // -1 for header offset

                // Meeting type
                val meetingType = when {
                    meeting.location != null -> "Standort"
                    meeting.event != null -> "Event"
                    else -> "Freies Gesuch"
                }

                // Meta line: Name • Ort • Art
                val parts = listOfNotNull(
                    meeting.author?.username,
                    meeting.author?.city,
                    meetingType
                ).filter { it.isNotEmpty() }
                holder.metaText.text = parts.joinToString(" • ")

                // Title
                holder.titleText.text = meeting.title ?: "Kein Titel"

                // Location name
                val locationName = when {
                    meeting.location != null -> meeting.location.titel ?: meeting.location.ort ?: ""
                    meeting.event != null -> meeting.event.title ?: ""
                    else -> ""
                }
                if (locationName.isNotEmpty()) {
                    holder.locationText.text = locationName
                    holder.locationText.visibility = View.VISIBLE
                } else {
                    holder.locationText.visibility = View.GONE
                }

                // Date info
                holder.dateText.text = formatMeetingDate(meeting)

                // Description
                if (!meeting.description.isNullOrEmpty()) {
                    holder.descriptionText.text = meeting.description
                    holder.descriptionText.visibility = View.VISIBLE
                } else {
                    holder.descriptionText.visibility = View.GONE
                }
            }
        }
    }

    private fun formatMeetingDate(meeting: Meeting): String {
        val dates = meeting.dates
        return when (dates?.type) {
            "fixed" -> {
                val date = (dates.value?.get("date") as? String) ?: ""
                "Termin: ${formatDate(date)}"
            }
            "range" -> {
                val start = (dates.value?.get("start") as? String) ?: ""
                val end = (dates.value?.get("end") as? String) ?: ""
                "Zeitraum: ${formatDate(start)} - ${formatDate(end)}"
            }
            "recurring" -> {
                val frequency = (dates.value?.get("frequency") as? String) ?: ""
                val days = (dates.value?.get("days") as? List<*>)?.joinToString(", ") ?: ""
                "Wiederkehrend: $days ($frequency)"
            }
            "eventDays" -> {
                val days = (dates.value?.get("days") as? List<*>)?.joinToString(", ") { formatDate(it.toString()) } ?: ""
                "Event-Tage: $days"
            }
            else -> meeting.date?.let { "Datum: ${formatDate(it)}" } ?: "Kein Datum"
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateStr)
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            try {
                val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
                val date = simpleFormat.parse(dateStr)
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                date?.let { outputFormat.format(it) } ?: dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Meeting>() {
        override fun areItemsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem == newItem
        }
    }
}
