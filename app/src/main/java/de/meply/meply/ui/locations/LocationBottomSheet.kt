package de.meply.meply.ui.locations

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.locations.*
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationBottomSheet : BottomSheetDialogFragment() {

    private lateinit var sheetTitle: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var titleInput: TextInputEditText
    private lateinit var typeSpinner: AutoCompleteTextView
    private lateinit var streetInput: TextInputEditText
    private lateinit var houseNumberInput: TextInputEditText
    private lateinit var zipInput: TextInputEditText
    private lateinit var cityInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var websiteInput: TextInputEditText
    private lateinit var allowUserEventsCheckbox: CheckBox
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private var editingLocation: Location? = null
    private var onLocationSaved: (() -> Unit)? = null

    companion object {
        private const val TAG = "LocationBottomSheet"
        private const val ARG_LOCATION_DOCUMENT_ID = "location_document_id"

        fun newInstance(locationDocumentId: String? = null): LocationBottomSheet {
            return LocationBottomSheet().apply {
                arguments = Bundle().apply {
                    locationDocumentId?.let { putString(ARG_LOCATION_DOCUMENT_ID, it) }
                }
            }
        }
    }

    fun setOnLocationSavedListener(listener: () -> Unit) {
        onLocationSaved = listener
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
        return inflater.inflate(R.layout.bottom_sheet_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupTypeSpinner()
        setupListeners()

        // Load location data if editing
        val documentId = arguments?.getString(ARG_LOCATION_DOCUMENT_ID)
        if (documentId != null) {
            setLoading(true)
            loadLocation(documentId)
        } else {
            updateUI()
        }
    }

    private fun initializeViews(view: View) {
        sheetTitle = view.findViewById(R.id.sheetTitle)
        closeButton = view.findViewById(R.id.closeButton)
        titleInput = view.findViewById(R.id.titleInput)
        typeSpinner = view.findViewById(R.id.typeSpinner)
        streetInput = view.findViewById(R.id.streetInput)
        houseNumberInput = view.findViewById(R.id.houseNumberInput)
        zipInput = view.findViewById(R.id.zipInput)
        cityInput = view.findViewById(R.id.cityInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        phoneInput = view.findViewById(R.id.phoneInput)
        emailInput = view.findViewById(R.id.emailInput)
        websiteInput = view.findViewById(R.id.websiteInput)
        allowUserEventsCheckbox = view.findViewById(R.id.allowUserEventsCheckbox)
        submitButton = view.findViewById(R.id.submitButton)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupTypeSpinner() {
        val types = LocationType.getAllTypes()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        typeSpinner.setAdapter(adapter)

        // Set default value
        if (typeSpinner.text.isNullOrBlank()) {
            typeSpinner.setText(types.first(), false)
        }
    }

    private fun setupListeners() {
        closeButton.setOnClickListener {
            tryDismiss()
        }

        submitButton.setOnClickListener {
            saveLocation()
        }
    }

    private fun updateUI() {
        if (editingLocation != null) {
            sheetTitle.text = "Location bearbeiten"
            submitButton.text = "Änderungen speichern"
            populateForm(editingLocation!!)
        } else {
            sheetTitle.text = "Neue Location"
            submitButton.text = "Location erstellen"
        }
    }

    private fun populateForm(location: Location) {
        titleInput.setText(location.titel ?: "")
        typeSpinner.setText(location.typ ?: LocationType.getAllTypes().first(), false)
        streetInput.setText(location.strasse ?: "")
        houseNumberInput.setText(location.hausnummer ?: "")
        zipInput.setText(location.plz ?: "")
        cityInput.setText(location.ort ?: "")
        descriptionInput.setText(location.beschreibung ?: "")
        phoneInput.setText(location.telefon ?: "")
        emailInput.setText(location.mail ?: "")
        websiteInput.setText(location.website ?: "")
        allowUserEventsCheckbox.isChecked = location.allowUserEvents == true
    }

    private fun loadLocation(documentId: String) {
        ApiClient.retrofit.getLocation(documentId).enqueue(object : Callback<LocationSingleResponse> {
            override fun onResponse(
                call: Call<LocationSingleResponse>,
                response: Response<LocationSingleResponse>
            ) {
                if (!isAdded) return

                setLoading(false)

                if (response.isSuccessful) {
                    editingLocation = response.body()?.data
                    Log.d(TAG, "Loaded location: ${editingLocation?.titel}, type: ${editingLocation?.typ}")
                    updateUI()
                } else {
                    Log.e(TAG, "Error loading location: ${response.code()}")
                    Toast.makeText(context, "Fehler beim Laden der Location", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LocationSingleResponse>, t: Throwable) {
                if (!isAdded) return

                setLoading(false)
                Log.e(TAG, "Network error loading location", t)
                Toast.makeText(context, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hasUnsavedContent(): Boolean {
        return !titleInput.text.isNullOrBlank() ||
               !streetInput.text.isNullOrBlank() ||
               !cityInput.text.isNullOrBlank() ||
               !descriptionInput.text.isNullOrBlank()
    }

    private fun showDiscardConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Änderungen verwerfen?")
            .setMessage("Du hast ungespeicherte Änderungen. Möchtest du wirklich abbrechen?")
            .setNegativeButton("Weiter bearbeiten", null)
            .setPositiveButton("Verwerfen") { _, _ ->
                dismiss()
            }
            .show()
    }

    fun tryDismiss() {
        if (hasUnsavedContent() && editingLocation == null) {
            showDiscardConfirmation()
        } else {
            dismiss()
        }
    }

    private fun saveLocation() {
        val title = titleInput.text?.toString()?.trim()
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Bitte gib einen Namen ein", Toast.LENGTH_SHORT).show()
            return
        }

        val type = typeSpinner.text?.toString()?.trim()
        if (type.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Bitte wähle einen Typ aus", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        val locationData = LocationData(
            titel = title,
            strasse = streetInput.text?.toString()?.trim()?.ifEmpty { null },
            hausnummer = houseNumberInput.text?.toString()?.trim()?.ifEmpty { null },
            plz = zipInput.text?.toString()?.trim()?.ifEmpty { null },
            ort = cityInput.text?.toString()?.trim()?.ifEmpty { null },
            typ = type,
            beschreibung = descriptionInput.text?.toString()?.trim()?.ifEmpty { null },
            mail = emailInput.text?.toString()?.trim()?.ifEmpty { null },
            website = websiteInput.text?.toString()?.trim()?.ifEmpty { null },
            telefon = phoneInput.text?.toString()?.trim()?.ifEmpty { null },
            allowUserEvents = allowUserEventsCheckbox.isChecked,
            coordinates = null // Coordinates would need geocoding - not implemented in app
        )

        val request = CreateLocationRequest(data = locationData)

        val call = if (editingLocation != null) {
            ApiClient.retrofit.updateLocation(editingLocation!!.documentId!!, request)
        } else {
            ApiClient.retrofit.createLocation(request)
        }

        call.enqueue(object : Callback<LocationActionResponse> {
            override fun onResponse(
                call: Call<LocationActionResponse>,
                response: Response<LocationActionResponse>
            ) {
                setLoading(false)

                if (response.isSuccessful) {
                    val message = if (editingLocation != null) {
                        "Location aktualisiert"
                    } else {
                        "Location erstellt"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    onLocationSaved?.invoke()
                    dismiss()
                } else {
                    val errorMessage = when (response.code()) {
                        403 -> "Keine Berechtigung"
                        400 -> "Ungültige Daten"
                        else -> "Fehler: ${response.code()}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error saving location: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LocationActionResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Network error saving location", t)
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        titleInput.isEnabled = !loading
        typeSpinner.isEnabled = !loading
        streetInput.isEnabled = !loading
        houseNumberInput.isEnabled = !loading
        zipInput.isEnabled = !loading
        cityInput.isEnabled = !loading
        descriptionInput.isEnabled = !loading
        phoneInput.isEnabled = !loading
        emailInput.isEnabled = !loading
        websiteInput.isEnabled = !loading
        allowUserEventsCheckbox.isEnabled = !loading
        closeButton.isEnabled = !loading
        isCancelable = !loading
    }
}
