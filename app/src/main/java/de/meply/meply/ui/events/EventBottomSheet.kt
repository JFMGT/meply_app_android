package de.meply.meply.ui.events

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
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
import com.google.android.material.textfield.TextInputLayout
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.events.*
import de.meply.meply.data.locations.Location
import de.meply.meply.data.locations.LocationsResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class EventBottomSheet : BottomSheetDialogFragment() {

    private lateinit var sheetTitle: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var urlInput: TextInputEditText
    private lateinit var fixedDateCheckbox: CheckBox
    private lateinit var datePlaceholderLayout: TextInputLayout
    private lateinit var datePlaceholderInput: TextInputEditText
    private lateinit var fixedDateContainer: LinearLayout
    private lateinit var startDateInput: TextInputEditText
    private lateinit var startTimeInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var endTimeInput: TextInputEditText
    private lateinit var repeatSpinner: AutoCompleteTextView
    private lateinit var streetInput: TextInputEditText
    private lateinit var streetNumberInput: TextInputEditText
    private lateinit var zipInput: TextInputEditText
    private lateinit var cityInput: TextInputEditText
    private lateinit var countryInput: TextInputEditText
    private lateinit var locationSpinner: AutoCompleteTextView
    private lateinit var publishCheckbox: CheckBox
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private var editingEvent: Event? = null
    private var onEventSaved: (() -> Unit)? = null

    // Date/time values stored as strings (yyyy-MM-dd / HH:mm:ss)
    private var startDate: String? = null
    private var startTime: String? = null
    private var endDate: String? = null
    private var endTime: String? = null

    // Available locations for linking
    private val availableLocations = mutableListOf<Location>()
    private var selectedLocationId: Int? = null

    companion object {
        private const val TAG = "EventBottomSheet"
        private const val ARG_EVENT_DOCUMENT_ID = "event_document_id"
        private var pendingEvent: Event? = null

        fun newInstance(): EventBottomSheet {
            return EventBottomSheet()
        }

        fun newInstanceWithEvent(event: Event): EventBottomSheet {
            pendingEvent = event
            return EventBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVENT_DOCUMENT_ID, event.documentId)
                }
            }
        }
    }

    fun setOnEventSavedListener(listener: () -> Unit) {
        onEventSaved = listener
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
        return inflater.inflate(R.layout.bottom_sheet_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRepeatSpinner()
        setupLocationSpinner()
        setupDateTimePickers()
        setupListeners()

        // Load event data if editing
        val documentId = arguments?.getString(ARG_EVENT_DOCUMENT_ID)
        if (documentId != null) {
            if (pendingEvent != null && pendingEvent?.documentId == documentId) {
                editingEvent = pendingEvent
                pendingEvent = null
                Log.d(TAG, "Using pending event: ${editingEvent?.title}")
                updateUI()
            } else {
                setLoading(true)
                loadEvent(documentId)
            }
        } else {
            updateUI()
        }

        // Load available locations
        loadLocations()
    }

    private fun initializeViews(view: View) {
        sheetTitle = view.findViewById(R.id.sheetTitle)
        closeButton = view.findViewById(R.id.closeButton)
        titleInput = view.findViewById(R.id.titleInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        urlInput = view.findViewById(R.id.urlInput)
        fixedDateCheckbox = view.findViewById(R.id.fixedDateCheckbox)
        datePlaceholderLayout = view.findViewById(R.id.datePlaceholderLayout)
        datePlaceholderInput = view.findViewById(R.id.datePlaceholderInput)
        fixedDateContainer = view.findViewById(R.id.fixedDateContainer)
        startDateInput = view.findViewById(R.id.startDateInput)
        startTimeInput = view.findViewById(R.id.startTimeInput)
        endDateInput = view.findViewById(R.id.endDateInput)
        endTimeInput = view.findViewById(R.id.endTimeInput)
        repeatSpinner = view.findViewById(R.id.repeatSpinner)
        streetInput = view.findViewById(R.id.streetInput)
        streetNumberInput = view.findViewById(R.id.streetNumberInput)
        zipInput = view.findViewById(R.id.zipInput)
        cityInput = view.findViewById(R.id.cityInput)
        countryInput = view.findViewById(R.id.countryInput)
        locationSpinner = view.findViewById(R.id.locationSpinner)
        publishCheckbox = view.findViewById(R.id.publishCheckbox)
        submitButton = view.findViewById(R.id.submitButton)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRepeatSpinner() {
        val repeatOptions = EventRepeat.entries.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, repeatOptions)
        repeatSpinner.setAdapter(adapter)
        repeatSpinner.setText(EventRepeat.NONE.displayName, false)
    }

    private fun setupLocationSpinner() {
        // Will be populated after loading locations
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, mutableListOf("Keine Location"))
        locationSpinner.setAdapter(adapter)
        locationSpinner.setText("Keine Location", false)

        locationSpinner.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                selectedLocationId = null
            } else if (position - 1 < availableLocations.size) {
                selectedLocationId = availableLocations[position - 1].id
            }
        }
    }

    private fun setupDateTimePickers() {
        val calendar = Calendar.getInstance()

        startDateInput.setOnClickListener {
            showDatePicker(calendar) { date ->
                startDate = date
                startDateInput.setText(formatDateForDisplay(date))
            }
        }

        startTimeInput.setOnClickListener {
            showTimePicker(calendar) { time ->
                startTime = time
                startTimeInput.setText(formatTimeForDisplay(time))
            }
        }

        endDateInput.setOnClickListener {
            showDatePicker(calendar) { date ->
                endDate = date
                endDateInput.setText(formatDateForDisplay(date))
            }
        }

        endTimeInput.setOnClickListener {
            showTimePicker(calendar) { time ->
                endTime = time
                endTimeInput.setText(formatTimeForDisplay(time))
            }
        }
    }

    private fun showDatePicker(calendar: Calendar, onDateSet: (String) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSet(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(calendar: Calendar, onTimeSet: (String) -> Unit) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val timeStr = String.format("%02d:%02d:00", hourOfDay, minute)
                onTimeSet(timeStr)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun formatDateForDisplay(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatTimeForDisplay(timeString: String?): String {
        if (timeString.isNullOrBlank()) return ""
        return timeString.take(5) // HH:mm
    }

    private fun setupListeners() {
        closeButton.setOnClickListener {
            tryDismiss()
        }

        fixedDateCheckbox.setOnCheckedChangeListener { _, isChecked ->
            fixedDateContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            datePlaceholderLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        submitButton.setOnClickListener {
            saveEvent()
        }
    }

    private fun updateUI() {
        if (editingEvent != null) {
            sheetTitle.text = "Event bearbeiten"
            submitButton.text = "Änderungen speichern"
            populateForm(editingEvent!!)
        } else {
            sheetTitle.text = "Neues Event"
            submitButton.text = "Event erstellen"
        }
    }

    private fun populateForm(event: Event) {
        titleInput.setText(event.title ?: "")
        descriptionInput.setText(event.description ?: "")
        urlInput.setText(event.url ?: "")

        val isFixedDate = event.fixedDate != false
        fixedDateCheckbox.isChecked = isFixedDate
        fixedDateContainer.visibility = if (isFixedDate) View.VISIBLE else View.GONE
        datePlaceholderLayout.visibility = if (isFixedDate) View.GONE else View.VISIBLE

        datePlaceholderInput.setText(event.datePlaceholder ?: "")

        // Set date/time values
        startDate = event.startDate
        startTime = event.startTime
        endDate = event.endDate
        endTime = event.endTime

        startDateInput.setText(formatDateForDisplay(event.startDate))
        startTimeInput.setText(formatTimeForDisplay(event.startTime))
        endDateInput.setText(formatDateForDisplay(event.endDate))
        endTimeInput.setText(formatTimeForDisplay(event.endTime))

        // Set repeat
        val repeat = EventRepeat.fromString(event.repeat)
        repeatSpinner.setText(repeat.displayName, false)

        // Set address
        streetInput.setText(event.street ?: "")
        streetNumberInput.setText(event.streetNumber ?: "")
        zipInput.setText(event.zip ?: "")
        cityInput.setText(event.city ?: "")
        countryInput.setText(event.country ?: "Deutschland")

        // Set location if linked
        event.location?.let { loc ->
            selectedLocationId = loc.id
            locationSpinner.setText(loc.titel ?: "Unbekannt", false)
        }

        // Set publish state
        publishCheckbox.isChecked = event.isPublished()
    }

    private fun loadEvent(documentId: String) {
        ApiClient.retrofit.getEvent(documentId).enqueue(object : Callback<EventSingleResponse> {
            override fun onResponse(
                call: Call<EventSingleResponse>,
                response: Response<EventSingleResponse>
            ) {
                if (!isAdded) return

                setLoading(false)

                if (response.isSuccessful) {
                    editingEvent = response.body()?.data
                    Log.d(TAG, "Loaded event: ${editingEvent?.title}")
                    updateUI()
                } else {
                    Log.e(TAG, "Error loading event: ${response.code()}")
                    Toast.makeText(context, "Fehler beim Laden des Events", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<EventSingleResponse>, t: Throwable) {
                if (!isAdded) return

                setLoading(false)
                Log.e(TAG, "Network error loading event", t)
                Toast.makeText(context, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadLocations() {
        val profileDocId = AuthManager.getProfileDocumentId(requireContext()) ?: return

        ApiClient.retrofit.getMyLocations(profileDocId).enqueue(object : Callback<LocationsResponse> {
            override fun onResponse(
                call: Call<LocationsResponse>,
                response: Response<LocationsResponse>
            ) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    availableLocations.clear()
                    availableLocations.addAll(response.body()?.data ?: emptyList())
                    updateLocationSpinner()
                }
            }

            override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                Log.e(TAG, "Error loading locations", t)
            }
        })
    }

    private fun updateLocationSpinner() {
        val options = mutableListOf("Keine Location")
        options.addAll(availableLocations.map { it.titel ?: "Unbenannt" })

        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, options)
        locationSpinner.setAdapter(adapter)

        // If editing and location was set, select it
        editingEvent?.location?.let { loc ->
            val index = availableLocations.indexOfFirst { it.id == loc.id }
            if (index >= 0) {
                locationSpinner.setText(options[index + 1], false)
                selectedLocationId = loc.id
            }
        }
    }

    private fun hasUnsavedContent(): Boolean {
        return !titleInput.text.isNullOrBlank() ||
               !descriptionInput.text.isNullOrBlank() ||
               !streetInput.text.isNullOrBlank() ||
               !cityInput.text.isNullOrBlank()
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
        if (hasUnsavedContent() && editingEvent == null) {
            showDiscardConfirmation()
        } else {
            dismiss()
        }
    }

    private fun saveEvent() {
        val title = titleInput.text?.toString()?.trim()
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Bitte gib einen Titel ein", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Get repeat value
        val repeatDisplayName = repeatSpinner.text?.toString() ?: ""
        val repeatValue = EventRepeat.entries.find { it.displayName == repeatDisplayName }?.value

        val eventData = EventData(
            title = title,
            description = descriptionInput.text?.toString()?.trim()?.ifEmpty { null },
            url = urlInput.text?.toString()?.trim()?.ifEmpty { null },
            fixedDate = fixedDateCheckbox.isChecked,
            startDate = if (fixedDateCheckbox.isChecked) startDate else null,
            startTime = if (fixedDateCheckbox.isChecked) startTime else null,
            endDate = if (fixedDateCheckbox.isChecked) endDate else null,
            endTime = if (fixedDateCheckbox.isChecked) endTime else null,
            datePlaceholder = if (!fixedDateCheckbox.isChecked) datePlaceholderInput.text?.toString()?.trim()?.ifEmpty { null } else null,
            repeat = if (repeatValue != "none") repeatValue else null,
            street = streetInput.text?.toString()?.trim()?.ifEmpty { null },
            streetNumber = streetNumberInput.text?.toString()?.trim()?.ifEmpty { null },
            zip = zipInput.text?.toString()?.trim()?.ifEmpty { null },
            city = cityInput.text?.toString()?.trim()?.ifEmpty { null },
            country = countryInput.text?.toString()?.trim()?.ifEmpty { null },
            locationId = selectedLocationId,
            coordinates = null
        )

        val request = CreateEventRequest(data = eventData)

        // Determine the API call based on edit/create and publish state
        val isEditing = editingEvent != null
        val shouldPublish = publishCheckbox.isChecked
        val wasPublished = editingEvent?.isPublished() ?: false

        val call = if (isEditing) {
            ApiClient.retrofit.updateEvent(editingEvent!!.documentId!!, request)
        } else {
            ApiClient.retrofit.createEvent(request)
        }

        call.enqueue(object : Callback<EventActionResponse> {
            override fun onResponse(
                call: Call<EventActionResponse>,
                response: Response<EventActionResponse>
            ) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    val savedEvent = response.body()?.data

                    // Handle publish/unpublish if needed
                    if (shouldPublish && savedEvent?.documentId != null && !wasPublished) {
                        publishEvent(savedEvent.documentId)
                    } else if (!shouldPublish && isEditing && wasPublished) {
                        unpublishEvent(editingEvent!!.documentId!!)
                    } else {
                        setLoading(false)
                        val message = if (isEditing) "Event aktualisiert" else "Event erstellt"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        onEventSaved?.invoke()
                        dismiss()
                    }
                } else {
                    setLoading(false)
                    val errorMessage = when (response.code()) {
                        403 -> "Keine Berechtigung"
                        400 -> "Ungültige Daten"
                        else -> "Fehler: ${response.code()}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error saving event: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<EventActionResponse>, t: Throwable) {
                if (!isAdded) return
                setLoading(false)
                Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Network error saving event", t)
            }
        })
    }

    private fun publishEvent(documentId: String) {
        ApiClient.retrofit.publishEvent(documentId).enqueue(object : Callback<EventActionResponse> {
            override fun onResponse(
                call: Call<EventActionResponse>,
                response: Response<EventActionResponse>
            ) {
                if (!isAdded) return
                setLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Event erstellt und veröffentlicht", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Event erstellt (Veröffentlichung fehlgeschlagen)", Toast.LENGTH_SHORT).show()
                }
                onEventSaved?.invoke()
                dismiss()
            }

            override fun onFailure(call: Call<EventActionResponse>, t: Throwable) {
                if (!isAdded) return
                setLoading(false)
                Toast.makeText(requireContext(), "Event erstellt (Veröffentlichung fehlgeschlagen)", Toast.LENGTH_SHORT).show()
                onEventSaved?.invoke()
                dismiss()
            }
        })
    }

    private fun unpublishEvent(documentId: String) {
        ApiClient.retrofit.unpublishEvent(documentId).enqueue(object : Callback<EventActionResponse> {
            override fun onResponse(
                call: Call<EventActionResponse>,
                response: Response<EventActionResponse>
            ) {
                if (!isAdded) return
                setLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Event aktualisiert und zurückgezogen", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Event aktualisiert", Toast.LENGTH_SHORT).show()
                }
                onEventSaved?.invoke()
                dismiss()
            }

            override fun onFailure(call: Call<EventActionResponse>, t: Throwable) {
                if (!isAdded) return
                setLoading(false)
                Toast.makeText(requireContext(), "Event aktualisiert", Toast.LENGTH_SHORT).show()
                onEventSaved?.invoke()
                dismiss()
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        titleInput.isEnabled = !loading
        descriptionInput.isEnabled = !loading
        urlInput.isEnabled = !loading
        fixedDateCheckbox.isEnabled = !loading
        datePlaceholderInput.isEnabled = !loading
        startDateInput.isEnabled = !loading
        startTimeInput.isEnabled = !loading
        endDateInput.isEnabled = !loading
        endTimeInput.isEnabled = !loading
        repeatSpinner.isEnabled = !loading
        streetInput.isEnabled = !loading
        streetNumberInput.isEnabled = !loading
        zipInput.isEnabled = !loading
        cityInput.isEnabled = !loading
        countryInput.isEnabled = !loading
        locationSpinner.isEnabled = !loading
        publishCheckbox.isEnabled = !loading
        closeButton.isEnabled = !loading
        isCancelable = !loading
    }
}
