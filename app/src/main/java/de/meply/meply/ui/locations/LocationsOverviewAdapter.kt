package de.meply.meply.ui.locations

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.locations.Location

class LocationsOverviewAdapter(
    private val onLocationClick: (Location) -> Unit
) : ListAdapter<LocationsOverviewAdapter.LocationWithDistance, LocationsOverviewAdapter.ViewHolder>(DiffCallback()) {

    /**
     * Location with optional distance info
     */
    data class LocationWithDistance(
        val location: Location,
        val distance: Double? = null
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_overview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.location_title)
        private val type: TextView = itemView.findViewById(R.id.location_type)
        private val address: TextView = itemView.findViewById(R.id.location_address)
        private val distanceContainer: LinearLayout = itemView.findViewById(R.id.distance_container)
        private val distance: TextView = itemView.findViewById(R.id.location_distance)
        private val description: TextView = itemView.findViewById(R.id.location_description)
        private val contactContainer: LinearLayout = itemView.findViewById(R.id.contact_container)
        private val website: TextView = itemView.findViewById(R.id.location_website)
        private val phone: TextView = itemView.findViewById(R.id.location_phone)

        fun bind(item: LocationWithDistance) {
            val loc = item.location

            // Title
            title.text = loc.titel ?: "Unbekannt"

            // Type badge
            type.text = loc.getLocalizedType()

            // Address
            address.text = loc.getFormattedAddress()

            // Distance
            if (item.distance != null) {
                distance.text = formatDistance(item.distance)
                distanceContainer.visibility = View.VISIBLE
            } else {
                distanceContainer.visibility = View.GONE
            }

            // Description
            if (!loc.beschreibung.isNullOrBlank()) {
                description.text = loc.beschreibung
                description.visibility = View.VISIBLE
            } else {
                description.visibility = View.GONE
            }

            // Contact info
            val hasWebsite = !loc.website.isNullOrBlank()
            val hasPhone = !loc.telefon.isNullOrBlank()

            if (hasWebsite || hasPhone) {
                contactContainer.visibility = View.VISIBLE

                if (hasWebsite) {
                    website.visibility = View.VISIBLE
                    website.setOnClickListener {
                        val url = if (loc.website!!.startsWith("http")) loc.website else "https://${loc.website}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        itemView.context.startActivity(intent)
                    }
                } else {
                    website.visibility = View.GONE
                }

                if (hasPhone) {
                    phone.visibility = View.VISIBLE
                    phone.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loc.telefon}"))
                        itemView.context.startActivity(intent)
                    }
                } else {
                    phone.visibility = View.GONE
                }
            } else {
                contactContainer.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener {
                onLocationClick(loc)
            }
        }

        private fun formatDistance(km: Double): String {
            return if (km < 1) {
                "< 1 km entfernt"
            } else {
                "${km.toInt()} km entfernt"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LocationWithDistance>() {
        override fun areItemsTheSame(oldItem: LocationWithDistance, newItem: LocationWithDistance): Boolean {
            return oldItem.location.documentId == newItem.location.documentId
        }

        override fun areContentsTheSame(oldItem: LocationWithDistance, newItem: LocationWithDistance): Boolean {
            return oldItem == newItem
        }
    }
}
