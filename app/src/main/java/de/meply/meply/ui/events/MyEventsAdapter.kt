package de.meply.meply.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.events.Event

class MyEventsAdapter(
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : ListAdapter<Event, MyEventsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventTitle: TextView = itemView.findViewById(R.id.event_title)
        private val eventStatus: TextView = itemView.findViewById(R.id.event_status)
        private val eventDate: TextView = itemView.findViewById(R.id.event_date)
        private val eventTime: TextView = itemView.findViewById(R.id.event_time)
        private val eventRepeat: TextView = itemView.findViewById(R.id.event_repeat)
        private val eventAddress: TextView = itemView.findViewById(R.id.event_address)
        private val eventLocation: TextView = itemView.findViewById(R.id.event_location)
        private val eventDescription: TextView = itemView.findViewById(R.id.event_description)
        private val locationContainer: LinearLayout = itemView.findViewById(R.id.location_container)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(event: Event) {
            eventTitle.text = event.title ?: "Unbenannt"

            if (event.isPublished()) {
                eventStatus.text = "Veröffentlicht"
                eventStatus.setBackgroundResource(R.drawable.badge_success_background)
            } else {
                eventStatus.text = "Entwurf"
                eventStatus.setBackgroundResource(R.drawable.badge_background)
            }

            val dateRange = event.getFormattedDateRange()
            eventDate.text = dateRange
            eventDate.visibility = if (dateRange.isNotEmpty() && dateRange != "Kein Datum") View.VISIBLE else View.GONE

            val timeRange = event.getFormattedTimeRange()
            if (timeRange.isNotEmpty()) {
                eventTime.text = timeRange
                eventTime.visibility = View.VISIBLE
            } else {
                eventTime.visibility = View.GONE
            }

            val repeatText = event.getRepeatText()
            if (repeatText.isNotEmpty()) {
                eventRepeat.text = repeatText
                eventRepeat.visibility = View.VISIBLE
            } else {
                eventRepeat.visibility = View.GONE
            }

            val address = event.getFormattedAddress()
            if (address != "–") {
                eventAddress.text = address
                eventAddress.visibility = View.VISIBLE
            } else {
                eventAddress.visibility = View.GONE
            }

            if (event.location?.titel != null) {
                eventLocation.text = event.location.titel
                locationContainer.visibility = View.VISIBLE
            } else {
                locationContainer.visibility = View.GONE
            }

            if (!event.description.isNullOrBlank()) {
                eventDescription.text = event.description
                eventDescription.visibility = View.VISIBLE
            } else {
                eventDescription.visibility = View.GONE
            }

            btnEdit.setOnClickListener { onEditClick(event) }
            btnDelete.setOnClickListener { onDeleteClick(event) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.documentId == newItem.documentId
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}
