package de.meply.meply.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.meply.meply.R

class PostImageAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: (List<String>, Int) -> Unit
) : RecyclerView.Adapter<PostImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.postImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            onImageClick(imageUrls, position)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
