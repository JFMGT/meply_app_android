package de.meply.meply.ui.collection

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import de.meply.meply.BaseDetailActivity
import de.meply.meply.R
import de.meply.meply.data.collection.*
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyCollectionActivity : BaseDetailActivity() {

    companion object {
        private const val TAG = "MyCollectionActivity"

        fun start(context: Context) {
            val intent = Intent(context, MyCollectionActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var searchInput: EditText
    private lateinit var searchResultsRecycler: RecyclerView
    private lateinit var noResultsHint: TextView
    private lateinit var collectionStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var gamesRecycler: RecyclerView

    private lateinit var collectionAdapter: CollectionAdapter
    private lateinit var searchAdapter: SearchResultsAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val games = mutableListOf<UserBoardgame>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_collection)

        setupToolbar()
        initializeViews()
        setupAdapters()
        setupSearch()
        loadCollection()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Meine Sammlung"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializeViews() {
        searchInput = findViewById(R.id.search_input)
        searchResultsRecycler = findViewById(R.id.search_results)
        noResultsHint = findViewById(R.id.no_results_hint)
        collectionStats = findViewById(R.id.collection_stats)
        loadingProgress = findViewById(R.id.loading_progress)
        emptyCard = findViewById(R.id.empty_card)
        gamesRecycler = findViewById(R.id.games_recycler)
    }

    private fun setupAdapters() {
        // Collection adapter
        collectionAdapter = CollectionAdapter(
            onRatingChanged = { game, rating -> updateGameRating(game, rating) },
            onStateChanged = { game, state -> updateGameState(game, state) }
        )
        gamesRecycler.layoutManager = LinearLayoutManager(this)
        gamesRecycler.adapter = collectionAdapter

        // Setup swipe gestures (right = sell, left = delete)
        setupSwipeGestures()

        // Search results adapter
        searchAdapter = SearchResultsAdapter { result ->
            addGameToCollection(result)
        }
        searchResultsRecycler.layoutManager = LinearLayoutManager(this)
        searchResultsRecycler.adapter = searchAdapter
    }

    private fun setupSwipeGestures() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            private val sellPaint = Paint().apply {
                color = ContextCompat.getColor(this@MyCollectionActivity, R.color.primary)
                isAntiAlias = true
            }
            private val deletePaint = Paint().apply {
                color = ContextCompat.getColor(this@MyCollectionActivity, R.color.error)
                isAntiAlias = true
            }
            private val sellIcon = ContextCompat.getDrawable(this@MyCollectionActivity, R.drawable.ic_tag)
            private val deleteIcon = ContextCompat.getDrawable(this@MyCollectionActivity, R.drawable.ic_trash)
            private val cornerRadius = 8 * resources.displayMetrics.density

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < games.size) {
                    val game = games[position]
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
                        icon.setTint(ContextCompat.getColor(this@MyCollectionActivity, R.color.text_on_primary))
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
                        icon.setTint(ContextCompat.getColor(this@MyCollectionActivity, R.color.white))
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
            loadCollection() // Refresh to get updated data
        }
        bottomSheet.show(supportFragmentManager, "sellGame")
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                if (query.length < 2) {
                    hideSearchResults()
                    return
                }

                searchRunnable = Runnable { searchGames(query) }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // TODO: Could create new game here
                true
            } else {
                false
            }
        }
    }

    private fun loadCollection() {
        loadingProgress.visibility = View.VISIBLE
        emptyCard.visibility = View.GONE
        gamesRecycler.visibility = View.GONE

        ApiClient.retrofit.getMyCollection(pageSize = 100)
            .enqueue(object : Callback<MyCollectionResponse> {
                override fun onResponse(
                    call: Call<MyCollectionResponse>,
                    response: Response<MyCollectionResponse>
                ) {
                    loadingProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        val collection = response.body()?.results ?: emptyList()
                        games.clear()
                        games.addAll(collection)
                        updateUI()
                    } else {
                        Log.e(TAG, "Error loading collection: ${response.code()}")
                        Toast.makeText(this@MyCollectionActivity, "Fehler beim Laden", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MyCollectionResponse>, t: Throwable) {
                    loadingProgress.visibility = View.GONE
                    Log.e(TAG, "Error loading collection: ${t.message}", t)
                    t.printStackTrace()
                    Toast.makeText(this@MyCollectionActivity, "Fehler: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun updateUI() {
        if (games.isEmpty()) {
            emptyCard.visibility = View.VISIBLE
            gamesRecycler.visibility = View.GONE
            collectionStats.text = "Keine Spiele in deiner Sammlung"
        } else {
            emptyCard.visibility = View.GONE
            gamesRecycler.visibility = View.VISIBLE
            collectionStats.text = "${games.size} Spiele in deiner Sammlung"
            collectionAdapter.submitList(games.toList())
        }
    }

    private fun searchGames(query: String) {
        ApiClient.retrofit.searchBoardgames(query)
            .enqueue(object : Callback<StrapiListResponse<BoardgameSearchResult>> {
                override fun onResponse(
                    call: Call<StrapiListResponse<BoardgameSearchResult>>,
                    response: Response<StrapiListResponse<BoardgameSearchResult>>
                ) {
                    if (response.isSuccessful) {
                        val results = response.body()?.data ?: emptyList()
                        if (results.isEmpty()) {
                            searchResultsRecycler.visibility = View.GONE
                            noResultsHint.visibility = View.VISIBLE
                        } else {
                            searchAdapter.submitList(results)
                            searchResultsRecycler.visibility = View.VISIBLE
                            noResultsHint.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<StrapiListResponse<BoardgameSearchResult>>, t: Throwable) {
                    Log.e(TAG, "Search error", t)
                }
            })
    }

    private fun hideSearchResults() {
        searchResultsRecycler.visibility = View.GONE
        noResultsHint.visibility = View.GONE
        searchAdapter.clear()
    }

    private fun addGameToCollection(result: BoardgameSearchResult) {
        hideSearchResults()
        searchInput.text.clear()

        val request = AddToCollectionRequest(boardgameId = result.id)
        ApiClient.retrofit.addToCollection(request)
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.alreadyExists == true) {
                            Toast.makeText(this@MyCollectionActivity, "Spiel bereits in deiner Sammlung", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MyCollectionActivity, "Spiel hinzugefügt!", Toast.LENGTH_SHORT).show()
                            loadCollection() // Reload to get the new game
                        }
                    } else {
                        Toast.makeText(this@MyCollectionActivity, "Fehler beim Hinzufügen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    Log.e(TAG, "Error adding game", t)
                    Toast.makeText(this@MyCollectionActivity, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateGameRating(game: UserBoardgame, rating: Int) {
        val request = UpdateUserBoardgameRequest(rating = rating.toFloat())
        ApiClient.retrofit.updateUserBoardgame(game.id, request)
            .enqueue(object : Callback<CollectionActionResponse> {
                override fun onResponse(
                    call: Call<CollectionActionResponse>,
                    response: Response<CollectionActionResponse>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@MyCollectionActivity, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
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
                    if (!response.isSuccessful) {
                        Toast.makeText(this@MyCollectionActivity, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CollectionActionResponse>, t: Throwable) {
                    Log.e(TAG, "Error updating state", t)
                }
            })
    }

    private fun confirmRemoveGame(game: UserBoardgame) {
        AlertDialog.Builder(this, R.style.Theme_Meply_AlertDialog)
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
                    if (response.isSuccessful) {
                        games.removeAll { it.id == game.id }
                        updateUI()
                        Toast.makeText(this@MyCollectionActivity, "Spiel entfernt", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MyCollectionActivity, "Fehler beim Entfernen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CollectionActionResponse>, t: Throwable) {
                    Log.e(TAG, "Error removing game", t)
                    Toast.makeText(this@MyCollectionActivity, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
