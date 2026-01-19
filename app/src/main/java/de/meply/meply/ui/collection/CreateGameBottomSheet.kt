package de.meply.meply.ui.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.collection.AddToCollectionRequest
import de.meply.meply.data.collection.AddToCollectionResponse
import de.meply.meply.data.collection.StrapiCreateBoardgameData
import de.meply.meply.data.collection.StrapiCreateBoardgameRequest
import de.meply.meply.data.collection.StrapiCreateBoardgameResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateGameBottomSheet : BottomSheetDialogFragment() {

    private var onGameCreatedListener: ((Int) -> Unit)? = null
    private var initialTitle: String = ""

    private lateinit var titleInput: TextInputEditText
    private lateinit var minPlayersInput: TextInputEditText
    private lateinit var maxPlayersInput: TextInputEditText
    private lateinit var minPlaytimeInput: TextInputEditText
    private lateinit var maxPlaytimeInput: TextInputEditText
    private lateinit var minAgeInput: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCancel: Button
    private lateinit var btnCreate: Button

    companion object {
        private const val ARG_INITIAL_TITLE = "initial_title"

        fun newInstance(initialTitle: String = ""): CreateGameBottomSheet {
            return CreateGameBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_TITLE, initialTitle)
                }
            }
        }
    }

    fun setOnGameCreatedListener(listener: (Int) -> Unit) {
        onGameCreatedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialTitle = arguments?.getString(ARG_INITIAL_TITLE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_create_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make bottom sheet full screen
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }

        titleInput = view.findViewById(R.id.titleInput)
        minPlayersInput = view.findViewById(R.id.minPlayersInput)
        maxPlayersInput = view.findViewById(R.id.maxPlayersInput)
        minPlaytimeInput = view.findViewById(R.id.minPlaytimeInput)
        maxPlaytimeInput = view.findViewById(R.id.maxPlaytimeInput)
        minAgeInput = view.findViewById(R.id.minAgeInput)
        progressBar = view.findViewById(R.id.progressBar)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnCreate = view.findViewById(R.id.btnCreate)

        // Pre-fill title if provided
        if (initialTitle.isNotEmpty()) {
            titleInput.setText(initialTitle)
        }

        setupListeners()
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnCreate.setOnClickListener {
            createGame()
        }
    }

    private fun createGame() {
        val title = titleInput.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte einen Titel eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.length < 2) {
            Toast.makeText(requireContext(), "Titel muss mindestens 2 Zeichen haben", Toast.LENGTH_SHORT).show()
            return
        }

        // Get optional fields
        val minPlayers = minPlayersInput.text?.toString()?.toIntOrNull()
        val maxPlayers = maxPlayersInput.text?.toString()?.toIntOrNull()
        val minAge = minAgeInput.text?.toString()?.toIntOrNull()

        showLoading(true)

        // Step 1: Create the boardgame directly in Strapi (like web version)
        // IMPORTANT: Use system token like web version (useSystemToken=true in PHP)
        // because regular users don't have permission to create boardgames directly
        val strapiData = StrapiCreateBoardgameData(
            title = title,
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            minAge = minAge
        )
        val request = StrapiCreateBoardgameRequest(data = strapiData)
        val systemToken = ApiClient.getSystemToken()

        ApiClient.retrofit.createBoardgameWithSystemToken(systemToken, request)
            .enqueue(object : Callback<StrapiCreateBoardgameResponse> {
                override fun onResponse(
                    call: Call<StrapiCreateBoardgameResponse>,
                    response: Response<StrapiCreateBoardgameResponse>
                ) {
                    val body = response.body()
                    val gameId = body?.data?.id

                    if (response.isSuccessful && gameId != null) {
                        // Step 2: Add the created game to user's collection
                        addGameToCollection(gameId, title)
                    } else {
                        showLoading(false)
                        val errorMsg = when (response.code()) {
                            400 -> "Ungueltige Daten"
                            401 -> "Nicht autorisiert"
                            403 -> "Keine Berechtigung"
                            404 -> "API-Endpunkt nicht gefunden"
                            else -> "Fehler beim Erstellen (${response.code()})"
                        }
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<StrapiCreateBoardgameResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Step 2: Add the created boardgame to the user's collection
     * This mirrors the web version's two-step approach
     */
    private fun addGameToCollection(gameId: Int, title: String) {
        val addRequest = AddToCollectionRequest(boardgameId = gameId)

        ApiClient.retrofit.addToCollection(addRequest)
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    showLoading(false)
                    val body = response.body()

                    if (response.isSuccessful && (body?.success == true || body?.id != null)) {
                        Toast.makeText(requireContext(), "Spiel \"$title\" erstellt und hinzugefuegt", Toast.LENGTH_SHORT).show()
                        onGameCreatedListener?.invoke(gameId)
                        dismiss()
                    } else if (body?.alreadyExists == true) {
                        Toast.makeText(requireContext(), "Spiel \"$title\" erstellt, war aber bereits in Sammlung", Toast.LENGTH_SHORT).show()
                        onGameCreatedListener?.invoke(gameId)
                        dismiss()
                    } else {
                        val errorMsg = body?.error ?: "Spiel erstellt, aber Zuweisung fehlgeschlagen"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Spiel erstellt, Zuweisung fehlgeschlagen: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnCreate.isEnabled = !loading
        btnCancel.isEnabled = !loading
        titleInput.isEnabled = !loading
        minPlayersInput.isEnabled = !loading
        maxPlayersInput.isEnabled = !loading
        minPlaytimeInput.isEnabled = !loading
        maxPlaytimeInput.isEnabled = !loading
        minAgeInput.isEnabled = !loading
    }
}
