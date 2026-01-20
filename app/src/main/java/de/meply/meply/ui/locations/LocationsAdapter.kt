package de.meply.meply.ui.locations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.locations.Location

class LocationsAdapter(
    private val onEditClick: (Location) -> Unit,
    private val onDeleteClick: (Location) -> Unit
) : ListAdapter<Location, LocationsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = getItem(position)
        holder.bind(location)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationIcon: ImageView = itemView.findViewById(R.id.location_icon)
        private val locationTitle: TextView = itemView.findViewById(R.id.location_title)
        private val locationType: TextView = itemView.findViewById(R.id.location_type)
        private val locationStatus: TextView = itemView.findViewById(R.id.location_status)
        private val locationAddress: TextView = itemView.findViewById(R.id.location_address)
        private val locationDescription: TextView = itemView.findViewById(R.id.location_description)
        private val contactContainer: LinearLayout = itemView.findViewById(R.id.contact_container)
        private val locationContact: TextView = itemView.findViewById(R.id.location_contact)
        private val locationUserEvents: TextView = itemView.findViewById(R.id.location_user_events)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(location: Location) {
            // Title
            locationTitle.text = location.titel ?: "Unbenannt"

            // Type
            locationType.text = location.getLocalizedType()

            // Type Icon
            val iconRes = when (location.typ) {
                "Geschäft" -> android.R.drawable.ic_menu_myplaces
                "Cafe" -> android.R.drawable.ic_menu_myplaces
                "Club" -> android.R.drawable.ic_menu_myplaces
                "Location" -> android.R.drawable.ic_menu_mapmode
                else -> android.R.drawable.ic_menu_mapmode
            }
            locationIcon.setImageResource(iconRes)

            // Status Badge
            if (location.isPublished()) {
                locationStatus.text = "Veröffentlicht"
                locationStatus.setBackgroundResource(R.drawable.badge_success_background)
            } else {
                locationStatus.text = "Entwurf"
                locationStatus.setBackgroundResource(R.drawable.badge_background)
            }

            // Address
            val address = location.getFormattedAddress()
            if (address != "–") {
                locationAddress.text = address
                locationAddress.visibility = View.VISIBLE
            } else {
                locationAddress.visibility = View.GONE
            }

            // Description
            if (!location.beschreibung.isNullOrBlank()) {
                locationDescription.text = location.beschreibung
                locationDescription.visibility = View.VISIBLE
            } else {
                locationDescription.visibility = View.GONE
            }

            // Contact Info
            val contactParts = mutableListOf<String>()
            location.mail?.let { if (it.isNotBlank()) contactParts.add(it) }
            location.telefon?.let { if (it.isNotBlank()) contactParts.add(it) }
            location.website?.let { if (it.isNotBlank()) contactParts.add(it) }

            if (contactParts.isNotEmpty()) {
                locationContact.text = contactParts.joinToString(" | ")
                contactContainer.visibility = View.VISIBLE
            } else {
                contactContainer.visibility = View.GONE
            }

            // Allow User Events Badge
            if (location.allowUserEvents == true) {
                locationUserEvents.visibility = View.VISIBLE
            } else {
                locationUserEvents.visibility = View.GONE
            }

            // Edit Button
            btnEdit.setOnClickListener {
                onEditClick(location)
            }

            // Delete Button
            btnDelete.setOnClickListener {
                onDeleteClick(location)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem == newItem
        }
    }
}
