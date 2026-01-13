package de.meply.meply.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.profile.SharedGame

class SharedGamesAdapter : RecyclerView.Adapter<SharedGamesAdapter.SharedGameViewHolder>() {

    private var games: List<SharedGame> = emptyList()

    fun submitList(newGames: List<SharedGame>) {
        games = newGames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedGameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_game, parent, false)
        return SharedGameViewHolder(view)
    }

    override fun onBindViewHolder(holder: SharedGameViewHolder, position: Int) {
        holder.bind(games[position])
    }

    override fun getItemCount(): Int = games.size

    class SharedGameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.game_title)
        private val ratingText: TextView = itemView.findViewById(R.id.game_rating)

        fun bind(game: SharedGame) {
            titleText.text = game.boardgame.title ?: "Unbekanntes Spiel"

            // Show rating from user B (the profile being viewed)
            val rating = game.ratingB ?: 0
            ratingText.text = renderStars(rating)
        }

        private fun renderStars(rating: Int): String {
            val stars = StringBuilder()
            for (i in 1..5) {
                stars.append(if (i <= rating) "★" else "☆")
            }
            return stars.toString()
        }
    }
}
