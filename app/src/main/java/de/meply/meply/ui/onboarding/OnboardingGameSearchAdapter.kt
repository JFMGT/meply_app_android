package de.meply.meply.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.meply.meply.R

class OnboardingGameSearchAdapter(
    private val onGameClick: (OnboardingGamesFragment.SimpleGame) -> Unit
) : RecyclerView.Adapter<OnboardingGameSearchAdapter.ViewHolder>() {

    private val games = mutableListOf<OnboardingGamesFragment.SimpleGame>()

    fun updateResults(newGames: List<OnboardingGamesFragment.SimpleGame>) {
        games.clear()
        games.addAll(newGames)
        notifyDataSetChanged()
    }

    fun removeGame(game: OnboardingGamesFragment.SimpleGame) {
        val index = games.indexOfFirst { it.documentId == game.documentId }
        if (index >= 0) {
            games.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_game_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(games[position])
    }

    override fun getItemCount() = games.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.game_image)
        private val name: TextView = itemView.findViewById(R.id.game_name)
        private val addButton: ImageView = itemView.findViewById(R.id.btn_add_game)

        fun bind(game: OnboardingGamesFragment.SimpleGame) {
            name.text = game.name

            if (!game.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(game.imageUrl)
                    .placeholder(R.drawable.ic_home)
                    .into(image)
            } else {
                image.setImageResource(R.drawable.ic_home)
            }

            addButton.setOnClickListener { onGameClick(game) }
            itemView.setOnClickListener { onGameClick(game) }
        }
    }
}
