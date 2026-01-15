package de.meply.meply.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.collection.BoardgameSearchResult

class SearchResultsAdapter(
    private val onItemClick: (BoardgameSearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    private var results: List<BoardgameSearchResult> = emptyList()

    fun submitList(newResults: List<BoardgameSearchResult>) {
        results = newResults
        notifyDataSetChanged()
    }

    fun clear() {
        results = emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.bind(result)
    }

    override fun getItemCount(): Int = results.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.search_result_title)

        fun bind(result: BoardgameSearchResult) {
            titleText.text = result.title ?: "Unbekannt"
            itemView.setOnClickListener {
                onItemClick(result)
            }
        }
    }
}
