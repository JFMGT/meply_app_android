package de.meply.meply.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.network.ApiClient
import de.meply.meply.data.collection.MyCollectionResponse
import de.meply.meply.data.collection.BoardgameSearchResult
import de.meply.meply.data.collection.AddToCollectionRequest
import de.meply.meply.data.collection.AddToCollectionResponse
import de.meply.meply.data.events.StrapiListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Handler
import android.os.Looper
import android.util.Log

class OnboardingGamesFragment : Fragment(), OnboardingStepValidator {

    private lateinit var editSearch: TextInputEditText
    private lateinit var searchResults: RecyclerView
    private lateinit var addedGames: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var counterText: TextView
    private lateinit var hintText: TextView

    private val addedGamesList = mutableListOf<SimpleGame>()
    private var searchAdapter: OnboardingGameSearchAdapter? = null
    private var addedAdapter: OnboardingAddedGamesAdapter? = null

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val MIN_GAMES = 10

    data class SimpleGame(
        val id: Int,
        val documentId: String,
        val name: String,
        val imageUrl: String?
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editSearch = view.findViewById(R.id.edit_game_search)
        searchResults = view.findViewById(R.id.search_results_recycler)
        addedGames = view.findViewById(R.id.added_games_recycler)
        progressBar = view.findViewById(R.id.search_progress)
        counterText = view.findViewById(R.id.games_counter)
        hintText = view.findViewById(R.id.games_hint)

        setupSearch()
        setupRecyclerViews()
        loadExistingCollection()
        updateCounter()
    }

    private fun setupSearch() {
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s.toString().trim()
                if (query.length >= 2) {
                    searchRunnable = Runnable { searchGames(query) }
                    searchHandler.postDelayed(searchRunnable!!, 500)
                } else {
                    searchAdapter?.updateResults(emptyList())
                }
            }
        })
    }

    private fun setupRecyclerViews() {
        // Search results
        searchAdapter = OnboardingGameSearchAdapter { game ->
            addGame(game)
        }
        searchResults.layoutManager = LinearLayoutManager(requireContext())
        searchResults.adapter = searchAdapter

        // Added games (horizontal)
        addedAdapter = OnboardingAddedGamesAdapter { game ->
            removeGame(game)
        }
        addedGames.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        addedGames.adapter = addedAdapter
    }

    private fun loadExistingCollection() {
        ApiClient.retrofit.getMyCollection()
            .enqueue(object : Callback<MyCollectionResponse> {
                override fun onResponse(
                    call: Call<MyCollectionResponse>,
                    response: Response<MyCollectionResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.results?.forEach { item ->
                            val simpleGame = SimpleGame(
                                id = item.id.toIntOrNull() ?: 0,
                                documentId = item.documentId ?: "",
                                name = item.title ?: "Unbekannt",
                                imageUrl = null // Collection items don't have thumbnail
                            )
                            if (!addedGamesList.any { it.documentId == simpleGame.documentId }) {
                                addedGamesList.add(simpleGame)
                            }
                        }
                        addedAdapter?.updateGames(addedGamesList)
                        updateCounter()
                    }
                }

                override fun onFailure(call: Call<MyCollectionResponse>, t: Throwable) {
                    Log.e("OnboardingGames", "Failed to load collection", t)
                }
            })
    }

    private fun searchGames(query: String) {
        progressBar.visibility = View.VISIBLE

        ApiClient.retrofit.searchBoardgames(query)
            .enqueue(object : Callback<StrapiListResponse<BoardgameSearchResult>> {
                override fun onResponse(
                    call: Call<StrapiListResponse<BoardgameSearchResult>>,
                    response: Response<StrapiListResponse<BoardgameSearchResult>>
                ) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val games = response.body()?.data?.map { game ->
                            SimpleGame(
                                id = game.id,
                                documentId = game.documentId ?: "",
                                name = game.title ?: "Unbekannt",
                                imageUrl = null // Search results don't have thumbnail
                            )
                        } ?: emptyList()

                        // Filter out already added games
                        val filteredGames = games.filter { game ->
                            !addedGamesList.any { it.id == game.id }
                        }
                        searchAdapter?.updateResults(filteredGames)
                    }
                }

                override fun onFailure(call: Call<StrapiListResponse<BoardgameSearchResult>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("OnboardingGames", "Search failed", t)
                }
            })
    }

    private fun addGame(game: SimpleGame) {
        // Add to local list
        addedGamesList.add(game)
        addedAdapter?.updateGames(addedGamesList)
        updateCounter()

        // Remove from search results
        searchAdapter?.removeGame(game)

        // Clear search
        editSearch.setText("")

        // Add to collection via API (uses numeric ID)
        val request = AddToCollectionRequest(boardgameId = game.id)
        ApiClient.retrofit.addToCollection(request)
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e("OnboardingGames", "Failed to add game: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    Log.e("OnboardingGames", "Failed to add game", t)
                }
            })
    }

    private fun removeGame(game: SimpleGame) {
        addedGamesList.remove(game)
        addedAdapter?.updateGames(addedGamesList)
        updateCounter()

        // Note: Removing from collection would require the user-boardgame entry ID
        // For onboarding simplicity, we just remove from the local list
    }

    private fun updateCounter() {
        val count = addedGamesList.size
        counterText.text = "$count/$MIN_GAMES Spiele hinzugefügt"

        if (count >= MIN_GAMES) {
            counterText.setTextColor(resources.getColor(R.color.success, null))
            hintText.visibility = View.GONE
        } else {
            counterText.setTextColor(resources.getColor(R.color.text_secondary, null))
            hintText.visibility = View.VISIBLE
            hintText.text = "Noch ${MIN_GAMES - count} Spiele hinzufügen"
        }
    }

    override fun canProceed(): Boolean {
        return addedGamesList.size >= MIN_GAMES
    }

    override fun showValidationError() {
        val remaining = MIN_GAMES - addedGamesList.size
        Toast.makeText(
            requireContext(),
            "Bitte füge noch $remaining Spiele hinzu",
            Toast.LENGTH_SHORT
        ).show()
    }
}
