package de.meply.meply.ui.uploads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import de.meply.meply.R
import de.meply.meply.data.uploads.UserUpload
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class UserUploadsAdapter(
    private val onImageClick: (UserUpload) -> Unit,
    private val onDeleteClick: (UserUpload) -> Unit
) : ListAdapter<UserUpload, UserUploadsAdapter.ViewHolder>(DiffCallback()) {

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val outputFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_upload, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val upload = getItem(position)
        holder.bind(upload)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.upload_thumbnail)
        private val name: TextView = itemView.findViewById(R.id.upload_name)
        private val details: TextView = itemView.findViewById(R.id.upload_details)
        private val dimensions: TextView = itemView.findViewById(R.id.upload_dimensions)
        private val reason: TextView = itemView.findViewById(R.id.upload_reason)
        private val date: TextView = itemView.findViewById(R.id.upload_date)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(upload: UserUpload) {
            val file = upload.file

            // Load thumbnail
            val thumbnailUrl = file?.thumbnailUrl ?: file?.url
            if (!thumbnailUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(thumbnailUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(thumbnail)
            } else {
                thumbnail.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // File name
            name.text = file?.name ?: "Unbekannt"

            // File details (mime + size)
            val mime = file?.mime ?: "–"
            val size = file?.getFormattedSize() ?: "– KB"
            details.text = "$mime • $size"

            // Dimensions
            dimensions.text = file?.getDimensions() ?: "–"

            // Reason
            val reasonText = getLocalizedReason(upload.reason)
            reason.text = "Grund: $reasonText"

            // Date
            date.text = formatDate(upload.createdAt)

            // Click on image for fullscreen view
            thumbnail.setOnClickListener {
                onImageClick(upload)
            }

            // Delete button
            deleteButton.setOnClickListener {
                onDeleteClick(upload)
            }
        }

        private fun getLocalizedReason(reason: String?): String {
            return when (reason) {
                "post" -> "Beitrag"
                "avatar" -> "Profilbild"
                "message" -> "Nachricht"
                else -> reason ?: "Unbekannt"
            }
        }

        private fun formatDate(isoDate: String?): String {
            if (isoDate.isNullOrEmpty()) return "–"
            return try {
                val parsed = inputFormat.parse(isoDate)
                if (parsed != null) outputFormat.format(parsed) else "–"
            } catch (e: Exception) {
                isoDate.substringBefore('T')
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UserUpload>() {
        override fun areItemsTheSame(oldItem: UserUpload, newItem: UserUpload): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserUpload, newItem: UserUpload): Boolean {
            return oldItem == newItem
        }
    }
}
