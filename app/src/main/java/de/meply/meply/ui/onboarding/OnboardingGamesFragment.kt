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
import de.meply.meply.data.collection.FindOrCreateBoardgameRequest
import de.meply.meply.data.collection.FindOrCreateBoardgameResponse
import de.meply.meply.data.events.StrapiListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText as MaterialTextInputEditText

class OnboardingGamesFragment : Fragment(), OnboardingStepValidator {

    private lateinit var editSearch: TextInputEditText
    private lateinit var searchResults: RecyclerView
    private lateinit var addedGames: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var counterText: TextView
    private lateinit var hintText: TextView
    private lateinit var btnCreateGame: Button

    private val displayedGamesList = mutableListOf<SimpleGame>()  // Games shown in horizontal list
    private var searchAdapter: OnboardingGameSearchAdapter? = null
    private var addedAdapter: OnboardingAddedGamesAdapter? = null
    private var existingCollectionCount: Int = 0  // Total games in collection from backend
    private var newlyAddedCount: Int = 0  // Games added in this session

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var currentSearchQuery: String = ""

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
        btnCreateGame = view.findViewById(R.id.btn_create_game)

        setupSearch()
        setupRecyclerViews()
        setupCreateGameButton()
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
                currentSearchQuery = query
                if (query.length >= 2) {
                    searchRunnable = Runnable { searchGames(query) }
                    searchHandler.postDelayed(searchRunnable!!, 500)
                } else {
                    searchAdapter?.updateResults(emptyList())
                    btnCreateGame.visibility = View.GONE
                }
            }
        })
    }

    private fun setupCreateGameButton() {
        btnCreateGame.setOnClickListener {
            showCreateGameDialog()
        }
    }

    private fun showCreateGameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_game, null)
        val editTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_game_title)

        // Pre-fill with current search query
        editTitle.setText(currentSearchQuery)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Neues Spiel erstellen")
            .setView(dialogView)
            .setPositiveButton("Erstellen") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    createNewGame(title)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun createNewGame(title: String) {
        progressBar.visibility = View.VISIBLE

        val request = FindOrCreateBoardgameRequest(
            title = title,
            isManualCreation = true
        )

        ApiClient.retrofit.findOrCreateBoardgame(request)
            .enqueue(object : Callback<FindOrCreateBoardgameResponse> {
                override fun onResponse(
                    call: Call<FindOrCreateBoardgameResponse>,
                    response: Response<FindOrCreateBoardgameResponse>
                ) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        response.body()?.let { result ->
                            val newGame = SimpleGame(
                                id = result.id,
                                documentId = "", // Will be filled by backend
                                name = title,
                                imageUrl = null
                            )
                            addGame(newGame)
                            Toast.makeText(
                                requireContext(),
                                if (result.existed) "Spiel gefunden und hinzugefügt" else "Spiel erstellt und hinzugefügt",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Fehler beim Erstellen des Spiels",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("OnboardingGames", "Create game failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<FindOrCreateBoardgameResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Netzwerkfehler beim Erstellen",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("OnboardingGames", "Create game failed", t)
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
        // Load first 10 games to display + get total count
        ApiClient.retrofit.getMyCollection(page = 1, pageSize = 10)
            .enqueue(object : Callback<MyCollectionResponse> {
                override fun onResponse(
                    call: Call<MyCollectionResponse>,
                    response: Response<MyCollectionResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        // Use the total count from pagination
                        existingCollectionCount = body?.pagination?.total ?: 0
                        Log.d("OnboardingGames", "Existing collection count: $existingCollectionCount")

                        // Load first 10 games into the display list
                        body?.results?.forEach { item ->
                            val simpleGame = SimpleGame(
                                id = item.id.toIntOrNull() ?: 0,
                                documentId = item.documentId ?: "",
                                name = item.title ?: "Unbekannt",
                                imageUrl = null
                            )
                            if (!displayedGamesList.any { it.id == simpleGame.id }) {
                                displayedGamesList.add(simpleGame)
                            }
                        }
                        addedAdapter?.updateGames(displayedGamesList)
                        updateCounter()
                    }
                }

                override fun onFailure(call: Call<MyCollectionResponse>, t: Throwable) {
                    Log.e("OnboardingGames", "Failed to load collection count", t)
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
                            !displayedGamesList.any { it.id == game.id }
                        }
                        searchAdapter?.updateResults(filteredGames)

                        // Show create button if no results found
                        btnCreateGame.visibility = if (filteredGames.isEmpty() && query.length >= 2) {
                            btnCreateGame.text = "\"$query\" als neues Spiel erstellen"
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<StrapiListResponse<BoardgameSearchResult>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("OnboardingGames", "Search failed", t)
                    // Show create button on error too
                    if (query.length >= 2) {
                        btnCreateGame.text = "\"$query\" als neues Spiel erstellen"
                        btnCreateGame.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun addGame(game: SimpleGame) {
        progressBar.visibility = View.VISIBLE

        // Add to collection via API first
        val request = AddToCollectionRequest(boardgameId = game.id)
        ApiClient.retrofit.addToCollection(request)
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    progressBar.visibility = View.GONE
                    val body = response.body()

                    if (response.isSuccessful && body?.success == true) {
                        // Remove from search results
                        searchAdapter?.removeGame(game)

                        // Clear search
                        editSearch.setText("")

                        if (body.alreadyExists == true) {
                            // Game was already in collection - don't add to counter
                            Toast.makeText(
                                requireContext(),
                                "\"${game.name}\" ist bereits in deiner Sammlung",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // New game added - increment counter and add to display
                            newlyAddedCount++
                            displayedGamesList.add(0, game)  // Add at beginning of list
                            addedAdapter?.updateGames(displayedGamesList)
                            updateCounter()
                            Toast.makeText(
                                requireContext(),
                                "\"${game.name}\" hinzugefügt",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val errorMsg = body?.error ?: body?.message ?: "Fehler beim Hinzufügen"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("OnboardingGames", "Failed to add game: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Netzwerkfehler: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("OnboardingGames", "Failed to add game", t)
                }
            })
    }

    private fun removeGame(game: SimpleGame) {
        val index = displayedGamesList.indexOf(game)
        if (index >= 0) {
            displayedGamesList.removeAt(index)
            addedAdapter?.updateGames(displayedGamesList)

            // If this was a newly added game (in the first positions), decrement counter
            if (index < newlyAddedCount) {
                newlyAddedCount--
                updateCounter()
            }
        }

        // Note: Removing from collection would require the user-boardgame entry ID
        // For onboarding simplicity, we just remove from the local list
    }

    private fun updateCounter() {
        // Total = existing collection + newly added in this session
        val totalCount = existingCollectionCount + newlyAddedCount

        if (totalCount == 1) {
            counterText.text = "1 Spiel in deiner Sammlung"
        } else {
            counterText.text = "$totalCount Spiele in deiner Sammlung"
        }

        if (totalCount > 0) {
            counterText.setTextColor(resources.getColor(R.color.success, null))
            hintText.visibility = View.GONE
        } else {
            counterText.setTextColor(resources.getColor(R.color.text_secondary, null))
            hintText.visibility = View.VISIBLE
        }
    }

    override fun canProceed(): Boolean {
        // Games are optional - user can always proceed
        return true
    }

    override fun showValidationError() {
        // Not needed anymore - step is always valid
    }
}
