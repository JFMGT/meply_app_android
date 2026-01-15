package de.meply.meply.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.collection.UserBoardgame

class CollectionAdapter(
    private val onRatingChanged: (UserBoardgame, Int) -> Unit,
    private val onStateChanged: (UserBoardgame, String) -> Unit,
    private val onRemoveClick: (UserBoardgame) -> Unit
) : ListAdapter<UserBoardgame, CollectionAdapter.ViewHolder>(DiffCallback()) {

    private val states = listOf(
        "none" to "Keine Angabe",
        "wishlist" to "Will spielen",
        "played" to "Habe gespielt",
        "owned" to "Besitze"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_game, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = getItem(position)
        holder.bind(game)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.game_title)
        private val saleIndicator: ImageView = itemView.findViewById(R.id.sale_indicator)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)
        private val stars: List<ImageView> = listOf(
            itemView.findViewById(R.id.star1),
            itemView.findViewById(R.id.star2),
            itemView.findViewById(R.id.star3),
            itemView.findViewById(R.id.star4),
            itemView.findViewById(R.id.star5)
        )

        fun bind(game: UserBoardgame) {
            title.text = game.title ?: "Unbekanntes Spiel"

            // Sale indicator
            saleIndicator.visibility = if (game.forSale == true) View.VISIBLE else View.GONE

            // Rating stars
            val rating = game.rating?.toInt() ?: 0
            stars.forEachIndexed { index, star ->
                val starNumber = index + 1
                star.setImageResource(
                    if (starNumber <= rating) R.drawable.ic_star_filled
                    else R.drawable.ic_star_outline
                )
                star.setOnClickListener {
                    onRatingChanged(game, starNumber)
                    updateStars(starNumber)
                }
            }

            // Status text - show current state
            val currentState = states.find { it.first == game.state }?.second ?: "Keine Angabe"
            statusText.text = currentState

            // Click to show selection dialog
            statusText.setOnClickListener {
                showStateSelectionDialog(game)
            }

            // Remove button
            btnRemove.setOnClickListener {
                onRemoveClick(game)
            }
        }

        private fun updateStars(newRating: Int) {
            stars.forEachIndexed { index, star ->
                val starNumber = index + 1
                star.setImageResource(
                    if (starNumber <= newRating) R.drawable.ic_star_filled
                    else R.drawable.ic_star_outline
                )
            }
        }

        private fun showStateSelectionDialog(game: UserBoardgame) {
            val context = itemView.context
            val stateLabels = states.map { it.second }.toTypedArray()
            val currentIndex = states.indexOfFirst { it.first == game.state }.coerceAtLeast(0)

            AlertDialog.Builder(context, R.style.Theme_Meply_AlertDialog)
                .setTitle("Status auswählen")
                .setSingleChoiceItems(stateLabels, currentIndex) { dialog, which ->
                    val newState = states[which].first
                    if (newState != game.state) {
                        statusText.text = states[which].second
                        onStateChanged(game, newState)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UserBoardgame>() {
        override fun areItemsTheSame(oldItem: UserBoardgame, newItem: UserBoardgame): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserBoardgame, newItem: UserBoardgame): Boolean {
            return oldItem == newItem
        }
    }
}
