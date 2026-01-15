package de.meply.meply.ui.collection

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.collection.CollectionActionResponse
import de.meply.meply.data.collection.UpdateUserBoardgameRequest
import de.meply.meply.data.collection.UserBoardgame
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SellGameBottomSheet : BottomSheetDialogFragment() {

    private lateinit var sheetTitle: TextView
    private lateinit var gameTitle: TextView
    private lateinit var forSaleSwitch: SwitchMaterial
    private lateinit var saleDetailsContainer: LinearLayout
    private lateinit var priceInput: TextInputEditText
    private lateinit var conditionSpinner: Spinner
    private lateinit var deliverySpinner: Spinner
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var tradePossibleSwitch: SwitchMaterial
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageButton

    private var game: UserBoardgame? = null
    private var onSaleUpdated: (() -> Unit)? = null

    // Condition options (value -> display)
    private val conditionOptions = listOf(
        "New" to "Neu",
        "Like New" to "Wie neu",
        "Very Good" to "Sehr gut",
        "Good" to "Gut",
        "Used" to "Gebraucht"
    )

    // Delivery options (value -> display)
    private val deliveryOptions = listOf(
        "ShippingOnly" to "Nur Versand",
        "PickupOnly" to "Nur Abholung",
        "ShippingOrPickup" to "Versand oder Abholung"
    )

    companion object {
        private const val ARG_GAME_ID = "game_id"
        private const val ARG_GAME_TITLE = "game_title"
        private const val ARG_FOR_SALE = "for_sale"
        private const val ARG_PRICE = "price"
        private const val ARG_CONDITION = "condition"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_DELIVERY_OPTION = "delivery_option"
        private const val ARG_TRADE_POSSIBLE = "trade_possible"

        fun newInstance(game: UserBoardgame): SellGameBottomSheet {
            return SellGameBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_GAME_ID, game.id)
                    putString(ARG_GAME_TITLE, game.title)
                    putBoolean(ARG_FOR_SALE, game.forSale ?: false)
                    game.price?.let { putDouble(ARG_PRICE, it) }
                    putString(ARG_CONDITION, game.condition ?: "Good")
                    putString(ARG_DESCRIPTION, game.description ?: "")
                    putString(ARG_DELIVERY_OPTION, game.deliveryOption ?: "ShippingOrPickup")
                    putBoolean(ARG_TRADE_POSSIBLE, game.tradePossible ?: false)
                }
            }
        }
    }

    fun setOnSaleUpdatedListener(listener: () -> Unit) {
        onSaleUpdated = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            game = UserBoardgame(
                id = args.getString(ARG_GAME_ID) ?: "",
                documentId = null,
                title = args.getString(ARG_GAME_TITLE),
                rating = null,
                state = null,
                forSale = args.getBoolean(ARG_FOR_SALE),
                price = if (args.containsKey(ARG_PRICE)) args.getDouble(ARG_PRICE) else null,
                condition = args.getString(ARG_CONDITION),
                description = args.getString(ARG_DESCRIPTION),
                deliveryOption = args.getString(ARG_DELIVERY_OPTION),
                tradePossible = args.getBoolean(ARG_TRADE_POSSIBLE)
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false
            }
        }

        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_sell_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupSpinners()
        setupListeners()
        populateFields()
    }

    private fun initializeViews(view: View) {
        sheetTitle = view.findViewById(R.id.sheetTitle)
        gameTitle = view.findViewById(R.id.gameTitle)
        forSaleSwitch = view.findViewById(R.id.forSaleSwitch)
        saleDetailsContainer = view.findViewById(R.id.saleDetailsContainer)
        priceInput = view.findViewById(R.id.priceInput)
        conditionSpinner = view.findViewById(R.id.conditionSpinner)
        deliverySpinner = view.findViewById(R.id.deliverySpinner)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        tradePossibleSwitch = view.findViewById(R.id.tradePossibleSwitch)
        submitButton = view.findViewById(R.id.submitButton)
        progressBar = view.findViewById(R.id.progressBar)
        closeButton = view.findViewById(R.id.closeButton)
    }

    private fun setupSpinners() {
        // Condition spinner
        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            conditionOptions.map { it.second }
        )
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        conditionSpinner.adapter = conditionAdapter

        // Delivery spinner
        val deliveryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            deliveryOptions.map { it.second }
        )
        deliveryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deliverySpinner.adapter = deliveryAdapter
    }

    private fun setupListeners() {
        closeButton.setOnClickListener {
            dismiss()
        }

        forSaleSwitch.setOnCheckedChangeListener { _, isChecked ->
            saleDetailsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        submitButton.setOnClickListener {
            saveSaleData()
        }
    }

    private fun populateFields() {
        game?.let { g ->
            gameTitle.text = g.title ?: "Unbekanntes Spiel"
            forSaleSwitch.isChecked = g.forSale ?: false
            saleDetailsContainer.visibility = if (g.forSale == true) View.VISIBLE else View.GONE

            g.price?.let { priceInput.setText(it.toString()) }
            descriptionInput.setText(g.description ?: "")
            tradePossibleSwitch.isChecked = g.tradePossible ?: false

            // Set condition spinner
            val conditionIndex = conditionOptions.indexOfFirst { it.first == g.condition }
            if (conditionIndex >= 0) {
                conditionSpinner.setSelection(conditionIndex)
            } else {
                // Default to "Good"
                conditionSpinner.setSelection(3)
            }

            // Set delivery spinner
            val deliveryIndex = deliveryOptions.indexOfFirst { it.first == g.deliveryOption }
            if (deliveryIndex >= 0) {
                deliverySpinner.setSelection(deliveryIndex)
            } else {
                // Default to "ShippingOrPickup"
                deliverySpinner.setSelection(2)
            }
        }
    }

    private fun saveSaleData() {
        val gameId = game?.id ?: return

        val forSale = forSaleSwitch.isChecked
        val price = priceInput.text?.toString()?.toDoubleOrNull()
        val condition = conditionOptions.getOrNull(conditionSpinner.selectedItemPosition)?.first ?: "Good"
        val description = descriptionInput.text?.toString()?.trim()
        val deliveryOption = deliveryOptions.getOrNull(deliverySpinner.selectedItemPosition)?.first ?: "ShippingOrPickup"
        val tradePossible = tradePossibleSwitch.isChecked

        val request = UpdateUserBoardgameRequest(
            forSale = forSale,
            price = if (forSale) price else null,
            condition = if (forSale) condition else null,
            description = if (forSale && !description.isNullOrEmpty()) description else null,
            deliveryOption = if (forSale) deliveryOption else null,
            tradePossible = if (forSale) tradePossible else null
        )

        setLoading(true)

        ApiClient.retrofit.updateUserBoardgame(gameId, request)
            .enqueue(object : Callback<CollectionActionResponse> {
                override fun onResponse(
                    call: Call<CollectionActionResponse>,
                    response: Response<CollectionActionResponse>
                ) {
                    setLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            if (forSale) "Verkaufsangebot gespeichert" else "Verkaufsangebot entfernt",
                            Toast.LENGTH_SHORT
                        ).show()
                        onSaleUpdated?.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Fehler: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<CollectionActionResponse>, t: Throwable) {
                    setLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Netzwerkfehler: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("SellGameBottomSheet", "Save failed", t)
                }
            })
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        forSaleSwitch.isEnabled = !loading
        priceInput.isEnabled = !loading
        conditionSpinner.isEnabled = !loading
        deliverySpinner.isEnabled = !loading
        descriptionInput.isEnabled = !loading
        tradePossibleSwitch.isEnabled = !loading
        closeButton.isEnabled = !loading
        isCancelable = !loading
    }
}
