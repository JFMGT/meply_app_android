package de.meply.meply.ui.collection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
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
        private val statusSpinner: Spinner = itemView.findViewById(R.id.status_spinner)
        private val stars: List<ImageView> = listOf(
            itemView.findViewById(R.id.star1),
            itemView.findViewById(R.id.star2),
            itemView.findViewById(R.id.star3),
            itemView.findViewById(R.id.star4),
            itemView.findViewById(R.id.star5)
        )

        private var isBinding = false

        fun bind(game: UserBoardgame) {
            isBinding = true

            title.text = game.title ?: "Unbekanntes Spiel"

            // Sale indicator
            saleIndicator.visibility = if (game.forSale == true) View.VISIBLE else View.GONE

            // Rating stars
            val rating = game.rating?.toInt() ?: 0
            stars.forEachIndexed { index, star ->
                val starNumber = index + 1
                star.setImageResource(
                    if (starNumber <= rating) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )
                star.setOnClickListener {
                    onRatingChanged(game, starNumber)
                    updateStars(starNumber)
                }
            }

            // Status spinner
            setupSpinner(itemView.context, game)

            // Remove button
            btnRemove.setOnClickListener {
                onRemoveClick(game)
            }

            isBinding = false
        }

        private fun updateStars(newRating: Int) {
            stars.forEachIndexed { index, star ->
                val starNumber = index + 1
                star.setImageResource(
                    if (starNumber <= newRating) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )
            }
        }

        private fun setupSpinner(context: Context, game: UserBoardgame) {
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                states.map { it.second }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            statusSpinner.adapter = adapter

            // Set current state
            val currentIndex = states.indexOfFirst { it.first == game.state }
            if (currentIndex >= 0) {
                statusSpinner.setSelection(currentIndex)
            }

            statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!isBinding) {
                        val newState = states[position].first
                        if (newState != game.state) {
                            onStateChanged(game, newState)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
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
