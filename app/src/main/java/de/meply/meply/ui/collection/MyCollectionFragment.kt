package de.meply.meply.ui.collection

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
        private const val SEARCH_DEBOUNCE_MS = 400L
        private const val PAGE_SIZE = 25
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var searchInput: EditText
    private lateinit var collectionStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var gamesRecycler: RecyclerView
    private lateinit var statusCard: MaterialCardView
    private lateinit var statusSpinner: ProgressBar
    private lateinit var statusText: TextView

    private lateinit var collectionAdapter: CollectionAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var currentSearchQuery: String = ""

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
        setupSearch()
        setupScrollListener()
        loadCollection(resetList = true)
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        nestedScrollView = view.findViewById(R.id.nestedScrollView)
        searchInput = view.findViewById(R.id.search_input)
        collectionStats = view.findViewById(R.id.collection_stats)
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
            onStateChanged = { game, state -> updateGameState(game, state) },
            onRemoveClick = { game -> confirmRemoveGame(game) }
        )
        gamesRecycler.layoutManager = LinearLayoutManager(requireContext())
        gamesRecycler.adapter = collectionAdapter

        // Setup swipe to sell
        setupSwipeToSell()
    }

    private fun setupSwipeToSell() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private val backgroundColor = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.primary))
            private val sellIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_tag)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < displayedGames.size) {
                    val game = displayedGames[position]
                    // Reset the item position (don't actually remove it)
                    collectionAdapter.notifyItemChanged(position)
                    // Open sell bottom sheet
                    openSellBottomSheet(game)
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
                val iconMargin = (itemView.height - (sellIcon?.intrinsicHeight ?: 0)) / 2

                // Draw yellow background
                if (dX > 0) {
                    backgroundColor.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    backgroundColor.draw(c)

                    // Draw sell icon
                    sellIcon?.let { icon ->
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth

                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.setTint(ContextCompat.getColor(requireContext(), R.color.text_on_primary))
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

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""

                // Cancel pending search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Debounce: wait before sending API request
                searchRunnable = Runnable {
                    if (query != currentSearchQuery) {
                        currentSearchQuery = query
                        loadCollection(resetList = true)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, SEARCH_DEBOUNCE_MS)
            }
        })
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

        val searchTitle = currentSearchQuery.ifEmpty { null }

        ApiClient.retrofit.getMyCollection(
            page = currentPage,
            pageSize = PAGE_SIZE,
            title = searchTitle
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
        if (displayedGames.isEmpty()) {
            emptyCard.visibility = View.VISIBLE
            gamesRecycler.visibility = View.GONE
            statusCard.visibility = View.GONE
            if (currentSearchQuery.isNotEmpty()) {
                collectionStats.text = "Keine Treffer für \"$currentSearchQuery\""
            } else {
                collectionStats.text = "Keine Spiele in deiner Sammlung"
            }
        } else {
            emptyCard.visibility = View.GONE
            gamesRecycler.visibility = View.VISIBLE

            if (currentSearchQuery.isNotEmpty()) {
                collectionStats.text = "${displayedGames.size} von $totalGames Treffern für \"$currentSearchQuery\""
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
        AlertDialog.Builder(requireContext())
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
