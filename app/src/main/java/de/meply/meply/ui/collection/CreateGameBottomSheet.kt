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
import de.meply.meply.data.collection.CreateBoardgameData
import de.meply.meply.data.collection.CreateBoardgameRequest
import de.meply.meply.data.collection.CreateBoardgameResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateGameBottomSheet : BottomSheetDialogFragment() {

    private var onGameCreatedListener: ((Int) -> Unit)? = null
    private var initialTitle: String = ""

    private lateinit var titleInput: TextInputEditText
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

        showLoading(true)

        val request = CreateBoardgameRequest(CreateBoardgameData(title))

        ApiClient.retrofit.createBoardgame(request)
            .enqueue(object : Callback<CreateBoardgameResponse> {
                override fun onResponse(
                    call: Call<CreateBoardgameResponse>,
                    response: Response<CreateBoardgameResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val gameId = response.body()?.data?.id
                        if (gameId != null) {
                            Toast.makeText(requireContext(), "Spiel \"$title\" erstellt", Toast.LENGTH_SHORT).show()
                            onGameCreatedListener?.invoke(gameId)
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), "Fehler: Keine Spiel-ID erhalten", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> "Ungueltige Daten"
                            401 -> "Nicht autorisiert"
                            403 -> "Keine Berechtigung"
                            409 -> "Spiel existiert bereits"
                            else -> "Fehler beim Erstellen (${response.code()})"
                        }
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CreateBoardgameResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnCreate.isEnabled = !loading
        btnCancel.isEnabled = !loading
        titleInput.isEnabled = !loading
    }
}
