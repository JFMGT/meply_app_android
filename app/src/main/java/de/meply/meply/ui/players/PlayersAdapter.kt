package de.meply.meply.ui.players

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.meply.meply.R
import de.meply.meply.data.players.PlayerMatch
import de.meply.meply.network.ApiClient
import de.meply.meply.utils.AvatarUtils

class PlayersAdapter(
    private val onPlayerClick: (PlayerMatch) -> Unit
) : ListAdapter<PlayerMatch, PlayersAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val match = getItem(position)
        holder.bind(match)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.player_avatar)
        private val readyBadge: TextView = itemView.findViewById(R.id.ready_badge)
        private val name: TextView = itemView.findViewById(R.id.player_name)
        private val city: TextView = itemView.findViewById(R.id.player_city)
        private val distance: TextView = itemView.findViewById(R.id.player_distance)
        private val sharedGamesContainer: LinearLayout = itemView.findViewById(R.id.shared_games_container)
        private val sharedGames: TextView = itemView.findViewById(R.id.player_shared_games)
        private val score: TextView = itemView.findViewById(R.id.player_score)

        fun bind(match: PlayerMatch) {
            val profile = match.otherProfile

            // Name
            name.text = profile?.username ?: "Unbekannt"

            // City
            if (!profile?.city.isNullOrBlank()) {
                city.text = profile?.city
                city.visibility = View.VISIBLE
            } else {
                city.visibility = View.GONE
            }

            // Distance
            distance.text = match.getFormattedDistance()

            // Shared games
            val gamesCount = match.sharedGamesCount ?: 0
            if (gamesCount > 0) {
                sharedGames.text = "$gamesCount Spiele"
                sharedGamesContainer.visibility = View.VISIBLE
            } else {
                sharedGamesContainer.visibility = View.GONE
            }

            // Score
            score.text = "${match.getScorePercent()}%"

            // Ready to Play Badge
            if (match.isReadyToPlay()) {
                readyBadge.visibility = View.VISIBLE
            } else {
                readyBadge.visibility = View.GONE
            }

            // Avatar
            val avatarUrl = profile?.getAvatarUrl()
            val fullAvatarUrl = if (!avatarUrl.isNullOrEmpty()) {
                "${ApiClient.STRAPI_IMAGE_BASE}$avatarUrl"
            } else {
                AvatarUtils.getDefaultAvatarUrl(profile?.documentId ?: "default")
            }

            Glide.with(itemView.context)
                .load(fullAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(avatar)

            // Click listener
            itemView.setOnClickListener {
                onPlayerClick(match)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlayerMatch>() {
        override fun areItemsTheSame(oldItem: PlayerMatch, newItem: PlayerMatch): Boolean {
            return oldItem.otherProfile?.documentId == newItem.otherProfile?.documentId
        }

        override fun areContentsTheSame(oldItem: PlayerMatch, newItem: PlayerMatch): Boolean {
            return oldItem == newItem
        }
    }
}
