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
import de.meply.meply.data.collection.FindOrCreateBoardgameRequest
import de.meply.meply.data.collection.FindOrCreateBoardgameResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateGameBottomSheet : BottomSheetDialogFragment() {

    private var onGameCreatedListener: ((String) -> Unit)? = null
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

    fun setOnGameCreatedListener(listener: (String) -> Unit) {
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

        // Use find-or-create route: finds existing game or creates new one
        // Uses User JWT (no system token needed)
        val request = FindOrCreateBoardgameRequest(
            title = title,
            minPlayer = minPlayers,
            maxPlayer = maxPlayers,
            minAge = minAge,
            isManualCreation = true
        )

        ApiClient.retrofit.findOrCreateBoardgame(request)
            .enqueue(object : Callback<FindOrCreateBoardgameResponse> {
                override fun onResponse(
                    call: Call<FindOrCreateBoardgameResponse>,
                    response: Response<FindOrCreateBoardgameResponse>
                ) {
                    val body = response.body()
                    val gameId = body?.id

                    if (response.isSuccessful && gameId != null) {
                        // Step 2: Add game to user's collection (using numeric id)
                        addGameToCollection(gameId, title, body.existed)
                    } else {
                        showLoading(false)
                        val errorMsg = when (response.code()) {
                            400 -> "Ungueltige Daten"
                            401 -> "Nicht autorisiert"
                            403 -> "Keine Berechtigung"
                            404 -> "API-Endpunkt nicht gefunden"
                            else -> "Fehler beim Erstellen"
                        }
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FindOrCreateBoardgameResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Verbindungsfehler. Bitte versuche es erneut.", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun addGameToCollection(gameId: Int, title: String, existed: Boolean) {
        // API expects numeric id, not documentId
        val addRequest = AddToCollectionRequest(boardgameId = gameId)

        ApiClient.retrofit.addToCollection(addRequest)
            .enqueue(object : Callback<AddToCollectionResponse> {
                override fun onResponse(
                    call: Call<AddToCollectionResponse>,
                    response: Response<AddToCollectionResponse>
                ) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val message = if (existed) {
                            "Spiel \"$title\" zur Sammlung hinzugefuegt"
                        } else {
                            "Spiel \"$title\" erstellt und hinzugefuegt"
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        onGameCreatedListener?.invoke(gameId.toString())
                        dismiss()
                    } else {
                        val addResponse = response.body()
                        if (addResponse?.alreadyExists == true) {
                            Toast.makeText(requireContext(), "Spiel ist bereits in deiner Sammlung", Toast.LENGTH_SHORT).show()
                            onGameCreatedListener?.invoke(gameId.toString())
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), "Spiel erstellt, aber Hinzufuegen fehlgeschlagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<AddToCollectionResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Verbindungsfehler beim Hinzufuegen", Toast.LENGTH_SHORT).show()
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
