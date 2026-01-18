package de.meply.meply.ui.collection

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import de.meply.meply.R
import de.meply.meply.data.collection.*
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyCollectionFragment : Fragment() {

    companion object {
        private const val TAG = "MyCollectionFragment"
        private const val PAGE_SIZE = 25
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var collectionStats: TextView
    private lateinit var filterInfo: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var gamesRecycler: RecyclerView
    private lateinit var statusCard: MaterialCardView
    private lateinit var statusSpinner: ProgressBar
    private lateinit var statusText: TextView

    private lateinit var collectionAdapter: CollectionAdapter

    private var currentFilter: CollectionFilter = CollectionFilter()

    private val displayedGames = mutableListOf<UserBoardgame>()
    private var currentPage = 1
    private var totalGames = 0
    private var isLoading = false
    private var hasMorePages = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupAdapters()
        setupScrollListener()
        loadCollection(resetList = true)
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        nestedScrollView = view.findViewById(R.id.nestedScrollView)
        collectionStats = view.findViewById(R.id.collection_stats)
        filterInfo = view.findViewById(R.id.filter_info)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        gamesRecycler = view.findViewById(R.id.games_recycler)
        statusCard = view.findViewById(R.id.status_card)
        statusSpinner = view.findViewById(R.id.status_spinner)
        statusText = view.findViewById(R.id.status_text)

        swipeRefresh.setOnRefreshListener {
            loadCollection(resetList = true)
        }
    }

    private fun setupAdapters() {
        collectionAdapter = CollectionAdapter(
            onRatingChanged = { game, rating -> updateGameRating(game, rating) },
            onStateChanged = { game, state -> updateGameState(game, state) }
        )
        gamesRecycler.layoutManager = LinearLayoutManager(requireContext())
        gamesRecycler.adapter = collectionAdapter

        // Setup swipe gestures (right = sell, left = delete)
        setupSwipeGestures()
    }

    private fun setupSwipeGestures() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            private val sellPaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.primary)
                isAntiAlias = true
            }
            private val deletePaint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.error)
                isAntiAlias = true
            }
            private val sellIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_tag)
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)
            private val cornerRadius = 8 * resources.displayMetrics.density

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < displayedGames.size) {
                    val game = displayedGames[position]
                    // Reset the item position first
                    collectionAdapter.notifyItemChanged(position)

                    when (direction) {
                        ItemTouchHelper.RIGHT -> openSellBottomSheet(game)
                        ItemTouchHelper.LEFT -> confirmRemoveGame(game)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                if (dX > 0) {
                    // Swipe right - Sell (yellow background with rounded corners)
                    val iconMargin = (itemView.height - (sellIcon?.intrinsicHeight ?: 0)) / 2

                    val rect = RectF(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat()
                    )
                    c.drawRoundRect(rect, cornerRadius, cornerRadius, sellPaint)

                    sellIcon?.let { icon ->
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.setTint(ContextCompat.getColor(requireContext(), R.color.text_on_primary))
                        icon.draw(c)
                    }
                } else if (dX < 0) {
                    // Swipe left - Delete (red background with rounded corners)
                    val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2

                    val rect = RectF(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRoundRect(rect, cornerRadius, cornerRadius, deletePaint)

                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.setTint(ContextCompat.getColor(requireContext(), R.color.white))
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(gamesRecycler)
    }

    private fun openSellBottomSheet(game: UserBoardgame) {
        val bottomSheet = SellGameBottomSheet.newInstance(game)
        bottomSheet.setOnSaleUpdatedListener {
            // Update the game in the list
            val index = displayedGames.indexOfFirst { it.id == game.id }
            if (index >= 0) {
                // Refresh the collection to get updated data
                loadCollection(resetList = true)
            }
        }
        bottomSheet.show(parentFragmentManager, "sellGame")
    }

    fun showFilterBottomSheet() {
        val bottomSheet = CollectionFilterBottomSheet.newInstance(currentFilter)
        bottomSheet.setOnFilterAppliedListener { filter ->
            currentFilter = filter
            updateFilterInfo()
            loadCollection(resetList = true)
        }
        bottomSheet.show(parentFragmentManager, "collectionFilter")
    }

    private fun updateFilterInfo() {
        val filters = mutableListOf<String>()

        currentFilter.title?.let { filters.add("Titel: \"$it\"") }
        currentFilter.state?.let { state ->
            val stateText = when (state) {
                "wishlist" -> "Will spielen"
                "played" -> "Gespielt"
                "owned" -> "Besitze"
                else -> state
            }
            filters.add("Status: $stateText")
        }
        currentFilter.minRating?.let { filters.add("Mind. ${it.toInt()}★") }
        currentFilter.forSale?.let { forSale ->
            val text = if (forSale == "true") "Im Trödelmarkt" else "Nicht im Trödelmarkt"
            filters.add(text)
        }
        // Add sort info if not default
        if (currentFilter.sortBy == "rating") {
            filters.add("Sortiert nach Bewertung")
        }

        if (filters.isNotEmpty()) {
            filterInfo.text = "Filter: ${filters.joinToString(" | ")}"
            filterInfo.visibility = View.VISIBLE
        } else {
            filterInfo.visibility = View.GONE
        }
    }

    private fun setupScrollListener() {
        nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            // Only check when scrolling down
            if (scrollY > oldScrollY && hasMorePages && !isLoading) {
                val contentHeight = v.getChildAt(0).measuredHeight
                val scrollViewHeight = v.measuredHeight
                val scrollPosition = scrollY + scrollViewHeight

                // Load more when within 200px of the bottom
                if (scrollPosition >= contentHeight - 200) {
                    loadCollection(resetList = false)
                }
            }
        }
    }

    private fun loadCollection(resetList: Boolean) {
        if (isLoading) return
        isLoading = true

        if (resetList) {
            currentPage = 1
            hasMorePages = true
            displayedGames.clear()
            loadingProgress.visibility = View.VISIBLE
            emptyCard.visibility = View.GONE
            gamesRecycler.visibility = View.GONE
            statusCard.visibility = View.GONE
        } else {
            // Loading more - show status card with spinner
            statusCard.visibility = View.VISIBLE
            statusSpinner.visibility = View.VISIBLE
            statusText.text = "Es wird versucht, weitere Einträge zu laden..."
        }

        ApiClient.retrofit.getMyCollection(
            page = currentPage,
            pageSize = PAGE_SIZE,
            title = currentFilter.title,
            state = currentFilter.state,
            minRating = currentFilter.minRating,
            forSale = currentFilter.forSale,
            sortBy = currentFilter.sortBy
        ).enqueue(object : Callback<MyCollectionResponse> {
            override fun onResponse(
                call: Call<MyCollectionResponse>,
                response: Response<MyCollectionResponse>
            ) {
                if (!isAdded) return

                isLoading = false
                loadingProgress.visibility = View.GONE
                statusSpinner.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val body = response.body()
                    val newGames = body?.results ?: emptyList()
                    val pagination = body?.pagination

                    totalGames = pagination?.total ?: newGames.size
                    val pageCount = pagination?.pageCount ?: 1
                    hasMorePages = currentPage < pageCount

                    displayedGames.addAll(newGames)
                    currentPage++

                    updateUI()
                } else {
                    Log.e(TAG, "Error loading collection: ${response.code()}")
                    Toast.makeText(requireContext(), "Fehler beim Laden: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MyCollectionResponse>, t: Throwable) {
                if (!isAdded) return

                isLoading = false
                loadingProgress.visibility = View.GONE
                statusSpinner.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                Log.e(TAG, "Error loading collection: ${t.message}", t)
                Toast.makeText(requireContext(), "Fehler: ${t.message}", Toast.LENGTH_LONG).show()

                // Show error in status card
                if (displayedGames.isNotEmpty()) {
                    statusCard.visibility = View.VISIBLE
                    statusText.text = "Fehler beim Laden weiterer Einträge"
                }
            }
        })
    }

    private fun updateUI() {
        val hasFilters = currentFilter.title != null ||
                currentFilter.state != null ||
                currentFilter.minRating != null ||
                currentFilter.forSale != null

        if (displayedGames.isEmpty()) {
            emptyCard.visibility = View.VISIBLE
            gamesRecycler.visibility = View.GONE
            statusCard.visibility = View.GONE
            if (hasFilters) {
                collectionStats.text = "Keine Treffer mit den aktuellen Filtern"
            } else {
                collectionStats.text = "Keine Spiele in deiner Sammlung"
            }
        } else {
            emptyCard.visibility = View.GONE
            gamesRecycler.visibility = View.VISIBLE

            if (hasFilters) {
                collectionStats.text = "${displayedGames.size} von $totalGames gefilterten Spielen"
            } else {
                collectionStats.text = "$totalGames Spiele in deiner Sammlung"
            }

            collectionAdapter.submitList(displayedGames.toList())

            // Update status card
            statusCard.visibility = View.VISIBLE
            statusSpinner.visibility = View.GONE
            if (!hasMorePages) {
                // All loaded
                statusText.text = "${displayedGames.size} von $totalGames Spielen geladen - Keine weiteren Einträge"
            } else {
                // More available - will show loading text when triggered
                statusText.text = "Scrolle nach unten für weitere Einträge"
            }
        }
    }

    private fun updateGameRating(game: UserBoardgame, rating: Int) {
        val request = UpdateUserBoardgameRequest(rating = rating.toFloat())
        ApiClient.retrofit.updateUserBoardgame(game.id, request)
            .enqueue(object : Callback<CollectionActionResponse> {
                override fun onResponse(
                    call: Call<CollectionActionResponse>,
                    response: Response<CollectionActionResponse>
                ) {
                    if (!isAdded) return
                    if (!response.isSuccessful) {
                        Toast.makeText(requireContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CollectionActionResponse>, t: Throwable) {
                    Log.e(TAG, "Error updating rating", t)
                }
            })
    }

    private fun updateGameState(game: UserBoardgame, state: String) {
        val request = UpdateUserBoardgameRequest(state = state)
        ApiClient.retrofit.updateUserBoardgame(game.id, request)
            .enqueue(object : Callback<CollectionActionResponse> {
                override fun onResponse(
                    call: Call<CollectionActionResponse>,
                    response: Response<CollectionActionResponse>
                ) {
                    if (!isAdded) return
                    if (!response.isSuccessful) {
                        Toast.makeText(requireContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CollectionActionResponse>, t: Throwable) {
                    Log.e(TAG, "Error updating state", t)
                }
            })
    }

    private fun confirmRemoveGame(game: UserBoardgame) {
        AlertDialog.Builder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Spiel entfernen")
            .setMessage("Möchtest du \"${game.title}\" wirklich aus deiner Sammlung entfernen?")
            .setPositiveButton("Entfernen") { _, _ ->
                removeGame(game)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun removeGame(game: UserBoardgame) {
        ApiClient.retrofit.removeFromCollection(game.id)
            .enqueue(object : Callback<CollectionActionResponse> {
                override fun onResponse(
                    call: Call<CollectionActionResponse>,
                    response: Response<CollectionActionResponse>
                ) {
                    if (!isAdded) return

                    if (response.isSuccessful) {
                        displayedGames.removeAll { it.id == game.id }
                        totalGames--
                        updateUI()
                        Toast.makeText(requireContext(), "Spiel entfernt", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Entfernen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CollectionActionResponse>, t: Throwable) {
                    if (!isAdded) return
                    Log.e(TAG, "Error removing game", t)
                    Toast.makeText(requireContext(), "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
