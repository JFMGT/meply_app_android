package de.meply.meply.ui.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.collection.AddToCollectionRequest
import de.meply.meply.data.collection.AddToCollectionResponse
import de.meply.meply.data.collection.BoardgameSearchResult
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddGameSearchBottomSheet : BottomSheetDialogFragment() {

    private var onGameAddedListener: (() -> Unit)? = null
    private lateinit var searchInput: TextInputEditText
    private lateinit var btnSearch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var searchResultsList: RecyclerView
    private lateinit var noResultsContainer: LinearLayout
    private lateinit var btnCreateGame: Button
    private lateinit var adapter: GameSearchAdapter

    private var lastSearchQuery: String = ""

    companion object {
        fun newInstance(): AddGameSearchBottomSheet {
            return AddGameSearchBottomSheet()
        }
    }

    fun setOnGameAddedListener(listener: () -> Unit) {
        onGameAddedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_game_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make bottom sheet full screen
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }

        searchInput = view.findViewById(R.id.searchInput)
        btnSearch = view.findViewById(R.id.btnSearch)
        progressBar = view.findViewById(R.id.progressBar)
        searchResultsList = view.findViewById(R.id.searchResultsList)
        noResultsContainer = view.findViewById(R.id.noResultsContainer)
        btnCreateGame = view.findViewById(R.id.btnCreateGame)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = GameSearchAdapter { game ->
            addGameToCollection(game)
        }
        searchResultsList.layoutManager = LinearLayoutManager(requireContext())
        searchResultsList.adapter = adapter
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            performSearch()
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        btnCreateGame.setOnClickListener {
            openCreateGameForm()
        }
    }

    private fun performSearch() {
        val query = searchInput.text?.toString()?.trim() ?: ""
        if (query.length < 2) {
            Toast.makeText(requireContext(), "Bitte mindestens 2 Zeichen eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        lastSearchQuery = query
        showLoading(true)
        noResultsContainer.visibility = View.GONE
        searchResultsList.visibility = View.GONE

        ApiClient.retrofit.searchBoardgames(query)
            .enqueue(object : Callback<StrapiListResponse<BoardgameSearchResult>> {
                override fun onResponse(
                    call: Call<StrapiListResponse<BoardgameSearchResult>>,
                    response: Response<StrapiListResponse<BoardgameSearchResult>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val results = response.body()?.data ?: emptyList()
                        if (results.isEmpty()) {
                            showNoResults()
                        } else {
                            showResults(results)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Fehler bei der Suche", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<StrapiListResponse<BoardgameSearchResult>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSearch.isEnabled = !loading
    }

    private fun showResults(results: List<BoardgameSearchResult>) {
        searchResultsList.visibility = View.VISIBLE
        noResultsContainer.visibility = View.GONE
        adapter.submitList(results)
    }

    private fun showNoResults() {
        searchResultsList.visibility = View.GONE
        noResultsContainer.visibility = View.VISIBLE
    }

    private fun addGameToCollection(game: BoardgameSearchResult) {
        showLoading(true)

        ApiClient.retrofit.addToCollection(AddToCollectionRequest(game.id))
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        if (body.alreadyExists == true) {
                            Toast.makeText(requireContext(), "\"${game.title}\" ist bereits in deiner Sammlung", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "\"${game.title}\" hinzugefuegt", Toast.LENGTH_SHORT).show()
                            onGameAddedListener?.invoke()
                            dismiss()
                        }
                    } else {
                        val errorMsg = body?.error ?: body?.message ?: "Fehler beim Hinzufuegen"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openCreateGameForm() {
        // Open the create game bottom sheet with the current search query as title
        val createSheet = CreateGameBottomSheet.newInstance(lastSearchQuery)
        createSheet.setOnGameCreatedListener { gameId ->
            // After creating a new game, add it to collection
            addGameToCollectionById(gameId)
        }
        createSheet.show(parentFragmentManager, "createGame")
    }

    private fun addGameToCollectionById(gameId: Int) {
        showLoading(true)

        ApiClient.retrofit.addToCollection(AddToCollectionRequest(gameId))
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Spiel zur Sammlung hinzugefuegt", Toast.LENGTH_SHORT).show()
                        onGameAddedListener?.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Hinzufuegen zur Sammlung", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

/**
 * Adapter for game search results
 */
class GameSearchAdapter(
    private val onAddClick: (BoardgameSearchResult) -> Unit
) : RecyclerView.Adapter<GameSearchAdapter.ViewHolder>() {

    private var items: List<BoardgameSearchResult> = emptyList()

    fun submitList(newItems: List<BoardgameSearchResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gameTitle = itemView.findViewById<android.widget.TextView>(R.id.gameTitle)
        private val btnAdd = itemView.findViewById<Button>(R.id.btnAdd)

        fun bind(game: BoardgameSearchResult) {
            gameTitle.text = game.title ?: "Unbekannt"
            btnAdd.setOnClickListener {
                onAddClick(game)
            }
        }
    }
}
