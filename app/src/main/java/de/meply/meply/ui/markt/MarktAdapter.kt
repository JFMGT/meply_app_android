package de.meply.meply.ui.markt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.markt.MarktGame
import de.meply.meply.data.markt.MarktOffer

class MarktAdapter(
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<MarktAdapter.GameViewHolder>() {

    private val games = mutableListOf<MarktGame>()
    private val expandedPositions = mutableSetOf<Int>()

    fun submit(list: List<MarktGame>) {
        games.clear()
        games.addAll(list)
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_markt_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        val isExpanded = expandedPositions.contains(position)
        holder.bind(game, isExpanded)
    }

    override fun getItemCount() = games.size

    inner class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val gameTitle: TextView = view.findViewById(R.id.gameTitle)
        private val offerCount: TextView = view.findViewById(R.id.offerCount)
        private val expandIcon: ImageView = view.findViewById(R.id.expandIcon)
        private val headerContainer: View = view.findViewById(R.id.headerContainer)
        private val offersContainer: LinearLayout = view.findViewById(R.id.offersContainer)

        fun bind(game: MarktGame, isExpanded: Boolean) {
            gameTitle.text = game.title ?: "Unbekanntes Spiel"

            val offers = game.offers ?: emptyList()
            offerCount.text = if (offers.size == 1) "1 Angebot" else "${offers.size} Angebote"

            // Set expand state
            updateExpandState(isExpanded, offers)

            // Toggle expand on header click
            headerContainer.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (expandedPositions.contains(currentPosition)) {
                        expandedPositions.remove(currentPosition)
                    } else {
                        expandedPositions.add(currentPosition)
                    }
                    notifyItemChanged(currentPosition)
                }
            }
        }

        private fun updateExpandState(isExpanded: Boolean, offers: List<MarktOffer>) {
            if (isExpanded) {
                expandIcon.setImageResource(R.drawable.ic_expand_less)
                offersContainer.visibility = View.VISIBLE
                offersContainer.removeAllViews()

                offers.forEachIndexed { index, offer ->
                    val offerView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_markt_offer, offersContainer, false)
                    bindOffer(offerView, offer, index == offers.lastIndex)
                    offersContainer.addView(offerView)
                }
            } else {
                expandIcon.setImageResource(R.drawable.ic_expand_more)
                offersContainer.visibility = View.GONE
                offersContainer.removeAllViews()
            }
        }

        private fun bindOffer(view: View, offer: MarktOffer, isLast: Boolean) {
            val userName: TextView = view.findViewById(R.id.userName)
            val userCity: TextView = view.findViewById(R.id.userCity)
            val price: TextView = view.findViewById(R.id.price)
            val condition: TextView = view.findViewById(R.id.condition)
            val delivery: TextView = view.findViewById(R.id.delivery)
            val tradePossible: TextView = view.findViewById(R.id.tradePossible)
            val description: TextView = view.findViewById(R.id.description)
            val divider: View = view.findViewById(R.id.divider)

            // User info - user is now a String (username)
            userName.text = offer.user ?: "Unbekannt"
            val city = offer.city
            if (!city.isNullOrEmpty()) {
                userCity.text = city
                userCity.visibility = View.VISIBLE
            } else {
                userCity.visibility = View.GONE
            }

            // Make user name clickable
            val userSlug = offer.userSlug
            if (userSlug != null) {
                userName.setOnClickListener { onUserClick(userSlug) }
            }

            // Price and condition
            price.text = offer.getFormattedPrice()
            condition.text = offer.getLocalizedCondition()

            // Delivery
            delivery.text = offer.getLocalizedDelivery()

            // Trade possible
            if (offer.tradePossible == true) {
                tradePossible.text = "Tausch möglich"
                tradePossible.visibility = View.VISIBLE
            } else {
                tradePossible.visibility = View.GONE
            }

            // Description
            if (!offer.description.isNullOrEmpty()) {
                description.text = offer.description
                description.visibility = View.VISIBLE
            } else {
                description.visibility = View.GONE
            }

            // Hide divider for last item
            divider.visibility = if (isLast) View.GONE else View.VISIBLE
        }
    }
}
