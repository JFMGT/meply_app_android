package de.meply.meply.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R

class OnboardingAddedGamesAdapter(
    private val onRemoveClick: (OnboardingGamesFragment.SimpleGame) -> Unit
) : RecyclerView.Adapter<OnboardingAddedGamesAdapter.ViewHolder>() {

    private val games = mutableListOf<OnboardingGamesFragment.SimpleGame>()

    fun updateGames(newGames: List<OnboardingGamesFragment.SimpleGame>) {
        games.clear()
        games.addAll(newGames)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_game_added, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(games[position])
    }

    override fun getItemCount() = games.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.game_image)
        private val name: TextView = itemView.findViewById(R.id.game_name)
        private val removeButton: ImageView = itemView.findViewById(R.id.btn_remove_game)

        fun bind(game: OnboardingGamesFragment.SimpleGame) {
            name.text = game.name
            // Always use dice icon
            image.setImageResource(R.drawable.ic_dice)
            removeButton.setOnClickListener { onRemoveClick(game) }
        }
    }
}
