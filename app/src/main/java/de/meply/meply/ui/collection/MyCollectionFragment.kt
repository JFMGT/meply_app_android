package de.meply.meply.ui.collection

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import de.meply.meply.R
import de.meply.meply.data.collection.*
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyCollectionFragment : Fragment() {

    companion object {
        private const val TAG = "MyCollectionFragment"
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
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
        loadCollection()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        searchInput = view.findViewById(R.id.search_input)
        searchResultsRecycler = view.findViewById(R.id.search_results)
        noResultsHint = view.findViewById(R.id.no_results_hint)
        collectionStats = view.findViewById(R.id.collection_stats)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        gamesRecycler = view.findViewById(R.id.games_recycler)

        swipeRefresh.setOnRefreshListener {
            loadCollection()
        }
    }

    private fun setupAdapters() {
        // Collection adapter
        collectionAdapter = CollectionAdapter(
            onRatingChanged = { game, rating -> updateGameRating(game, rating) },
            onStateChanged = { game, state -> updateGameState(game, state) },
            onRemoveClick = { game -> confirmRemoveGame(game) }
        )
        gamesRecycler.layoutManager = LinearLayoutManager(requireContext())
        gamesRecycler.adapter = collectionAdapter

        // Search results adapter
        searchAdapter = SearchResultsAdapter { result ->
            addGameToCollection(result)
        }
        searchResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecycler.adapter = searchAdapter
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
                    if (!isAdded) return

                    loadingProgress.visibility = View.GONE
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        val collection = response.body()?.results ?: emptyList()
                        games.clear()
                        games.addAll(collection)
                        updateUI()
                    } else {
                        Log.e(TAG, "Error loading collection: ${response.code()}")
                        Toast.makeText(requireContext(), "Fehler beim Laden: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MyCollectionResponse>, t: Throwable) {
                    if (!isAdded) return

                    loadingProgress.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    Log.e(TAG, "Error loading collection: ${t.message}", t)
                    Toast.makeText(requireContext(), "Fehler: ${t.message}", Toast.LENGTH_LONG).show()
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
                    if (!isAdded) return

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
                    if (!isAdded) return

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.alreadyExists == true) {
                            Toast.makeText(requireContext(), "Spiel bereits in deiner Sammlung", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Spiel hinzugefügt!", Toast.LENGTH_SHORT).show()
                            loadCollection()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Hinzufügen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    if (!isAdded) return
                    Log.e(TAG, "Error adding game", t)
                    Toast.makeText(requireContext(), "Netzwerkfehler", Toast.LENGTH_SHORT).show()
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
                        games.removeAll { it.id == game.id }
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
