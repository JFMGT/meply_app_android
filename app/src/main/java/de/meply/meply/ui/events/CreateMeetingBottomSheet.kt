package de.meply.meply.ui.events

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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.meeting.CreateMeetingRequest
import de.meply.meply.data.meeting.MeetingDataRequest
import de.meply.meply.data.meeting.MeetingDatesRequest
import de.meply.meply.data.meeting.MeetingResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class CreateMeetingBottomSheet : BottomSheetDialogFragment() {

    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var eventInfoCard: MaterialCardView
    private lateinit var eventInfoText: TextView
    private lateinit var eventDaysContainer: LinearLayout
    private lateinit var daysCheckboxContainer: LinearLayout
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageButton

    private var eventDocumentId: String? = null
    private var eventTitle: String? = null
    private var eventStartDate: String? = null
    private var eventEndDate: String? = null

    private val selectedDays = mutableSetOf<String>()
    private val allEventDays = mutableListOf<String>() // Track all available event days
    private var onMeetingCreated: (() -> Unit)? = null

    companion object {
        private const val ARG_EVENT_DOCUMENT_ID = "event_document_id"
        private const val ARG_EVENT_TITLE = "event_title"
        private const val ARG_EVENT_START_DATE = "event_start_date"
        private const val ARG_EVENT_END_DATE = "event_end_date"

        fun newInstance(
            eventDocumentId: String,
            eventTitle: String,
            eventStartDate: String?,
            eventEndDate: String?
        ): CreateMeetingBottomSheet {
            return CreateMeetingBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVENT_DOCUMENT_ID, eventDocumentId)
                    putString(ARG_EVENT_TITLE, eventTitle)
                    eventStartDate?.let { putString(ARG_EVENT_START_DATE, it) }
                    eventEndDate?.let { putString(ARG_EVENT_END_DATE, it) }
                }
            }
        }
    }

    fun setOnMeetingCreatedListener(listener: () -> Unit) {
        onMeetingCreated = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventDocumentId = arguments?.getString(ARG_EVENT_DOCUMENT_ID)
        eventTitle = arguments?.getString(ARG_EVENT_TITLE)
        eventStartDate = arguments?.getString(ARG_EVENT_START_DATE)
        eventEndDate = arguments?.getString(ARG_EVENT_END_DATE)
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

    private fun hasUnsavedContent(): Boolean {
        return !titleInput.text.isNullOrBlank() || !descriptionInput.text.isNullOrBlank()
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
        if (hasUnsavedContent()) {
            showDiscardConfirmation()
        } else {
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_create_meeting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupEventInfo()
        setupEventDays()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        titleInput = view.findViewById(R.id.titleInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        eventInfoCard = view.findViewById(R.id.eventInfoCard)
        eventInfoText = view.findViewById(R.id.eventInfoText)
        eventDaysContainer = view.findViewById(R.id.eventDaysContainer)
        daysCheckboxContainer = view.findViewById(R.id.daysCheckboxContainer)
        submitButton = view.findViewById(R.id.submitButton)
        progressBar = view.findViewById(R.id.progressBar)
        closeButton = view.findViewById(R.id.closeButton)
    }

    private fun setupEventInfo() {
        if (eventDocumentId != null && eventTitle != null) {
            eventInfoCard.visibility = View.VISIBLE
            eventInfoText.text = eventTitle
        } else {
            eventInfoCard.visibility = View.GONE
        }
    }

    private fun setupEventDays() {
        if (eventStartDate.isNullOrBlank()) {
            eventDaysContainer.visibility = View.GONE
            return
        }

        eventDaysContainer.visibility = View.VISIBLE
        daysCheckboxContainer.removeAllViews()

        val days = generateEventDays(eventStartDate!!, eventEndDate)

        if (days.isEmpty()) {
            eventDaysContainer.visibility = View.GONE
            return
        }

        // Store all event days for comparison later
        allEventDays.clear()
        allEventDays.addAll(days)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)
        val displayFormat = SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMANY)

        for (day in days) {
            val checkBox = CheckBox(requireContext()).apply {
                text = try {
                    val date = dateFormat.parse(day)
                    displayFormat.format(date!!)
                } catch (e: Exception) {
                    day
                }
                setTextColor(resources.getColor(R.color.text_primary, null))
                isChecked = true // Select all days by default
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedDays.add(day)
                    } else {
                        selectedDays.remove(day)
                    }
                }
            }
            selectedDays.add(day) // Add to selected by default
            daysCheckboxContainer.addView(checkBox)
        }
    }

    private fun generateEventDays(startDate: String, endDate: String?): List<String> {
        val days = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)

        try {
            val start = dateFormat.parse(startDate) ?: return days
            val end = if (!endDate.isNullOrBlank()) {
                dateFormat.parse(endDate) ?: start
            } else {
                start
            }

            val calendar = Calendar.getInstance()
            calendar.time = start

            // Limit to max 14 days to avoid too many checkboxes
            var dayCount = 0
            val maxDays = 14

            while (!calendar.time.after(end) && dayCount < maxDays) {
                days.add(dateFormat.format(calendar.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                dayCount++
            }
        } catch (e: Exception) {
            Log.e("CreateMeetingBottomSheet", "Error generating event days", e)
        }

        return days
    }

    private fun setupListeners() {
        closeButton.setOnClickListener {
            tryDismiss()
        }

        submitButton.setOnClickListener {
            createMeeting()
        }
    }

    private fun createMeeting() {
        val title = titleInput.text?.toString()?.trim()
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Bitte gib einen Titel ein", Toast.LENGTH_SHORT).show()
            return
        }

        val description = descriptionInput.text?.toString()?.trim()

        // Validate day selection if event days are shown
        if (eventDaysContainer.visibility == View.VISIBLE && selectedDays.isEmpty()) {
            Toast.makeText(requireContext(), "Bitte wähle mindestens einen Tag aus", Toast.LENGTH_SHORT).show()
            return
        }

        val profileId = AuthManager.getProfileDocumentId(requireContext())
        if (profileId == null) {
            Toast.makeText(requireContext(), "Nicht angemeldet", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Build dates object based on selected days
        val dates: MeetingDatesRequest
        val filterDate: String?

        if (eventDaysContainer.visibility == View.VISIBLE && selectedDays.isNotEmpty()) {
            // Check if all days are selected
            val allDaysSelected = selectedDays.size == allEventDays.size &&
                    selectedDays.containsAll(allEventDays)

            if (allDaysSelected) {
                // All event days selected - send empty map (displays "An allen Eventtagen")
                dates = MeetingDatesRequest(
                    type = "eventDays",
                    value = emptyMap()
                )
                filterDate = allEventDays.maxOrNull()
            } else {
                // Only specific days selected - send the selected days
                dates = MeetingDatesRequest(
                    type = "eventDays",
                    value = mapOf("days" to selectedDays.sorted())
                )
                filterDate = selectedDays.maxOrNull()
            }
        } else {
            // Fallback: all event days
            dates = MeetingDatesRequest(
                type = "eventDays",
                value = emptyMap()
            )
            filterDate = null
        }

        val meetingData = MeetingDataRequest(
            title = title,
            description = description?.ifEmpty { null },
            dates = dates,
            date = filterDate,
            author = profileId,
            event = eventDocumentId
        )

        val request = CreateMeetingRequest(data = meetingData)

        ApiClient.retrofit.createMeeting(request).enqueue(object : Callback<MeetingResponse> {
            override fun onResponse(call: Call<MeetingResponse>, response: Response<MeetingResponse>) {
                setLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Gesuch erstellt", Toast.LENGTH_SHORT).show()
                    onMeetingCreated?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Erstellen: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("CreateMeetingBottomSheet", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<MeetingResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("CreateMeetingBottomSheet", "Network error", t)
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        titleInput.isEnabled = !loading
        descriptionInput.isEnabled = !loading
        closeButton.isEnabled = !loading
        isCancelable = !loading
    }
}
