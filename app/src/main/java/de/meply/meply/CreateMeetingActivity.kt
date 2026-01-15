package de.meply.meply

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
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

class CreateMeetingActivity : AppCompatActivity() {

    private lateinit var inputTitle: TextInputEditText
    private lateinit var inputDescription: TextInputEditText
    private lateinit var spinnerDateType: Spinner
    private lateinit var layoutFixedDate: LinearLayout
    private lateinit var layoutDateRange: LinearLayout
    private lateinit var layoutRecurring: LinearLayout
    private lateinit var layoutEventDays: LinearLayout
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var btnSelectDateFrom: MaterialButton
    private lateinit var btnSelectDateTo: MaterialButton
    private lateinit var checkboxMonday: CheckBox
    private lateinit var checkboxTuesday: CheckBox
    private lateinit var checkboxWednesday: CheckBox
    private lateinit var checkboxThursday: CheckBox
    private lateinit var checkboxFriday: CheckBox
    private lateinit var checkboxSaturday: CheckBox
    private lateinit var checkboxSunday: CheckBox
    private lateinit var spinnerFrequency: Spinner
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var eventInfoCard: MaterialCardView
    private lateinit var eventInfoText: TextView

    private var selectedDate: Calendar? = null
    private var selectedDateFrom: Calendar? = null
    private var selectedDateTo: Calendar? = null

    // Event parameters (when creating meeting for a specific event)
    private var eventDocumentId: String? = null
    private var eventTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_meeting)

        // Get event parameters from intent
        eventDocumentId = intent.getStringExtra(EXTRA_EVENT_DOCUMENT_ID)
        eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE)

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (eventDocumentId != null) {
            "Gesuch für Event"
        } else {
            "Neues Gesuch erstellen"
        }

        initViews()
        setupDateTypeSpinner()
        setupFrequencySpinner()
        setupListeners()
        setupEventInfo()
    }

    private fun initViews() {
        inputTitle = findViewById(R.id.inputTitle)
        inputDescription = findViewById(R.id.inputDescription)
        spinnerDateType = findViewById(R.id.spinnerDateType)
        layoutFixedDate = findViewById(R.id.layoutFixedDate)
        layoutDateRange = findViewById(R.id.layoutDateRange)
        layoutRecurring = findViewById(R.id.layoutRecurring)
        layoutEventDays = findViewById(R.id.layoutEventDays)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectDateFrom = findViewById(R.id.btnSelectDateFrom)
        btnSelectDateTo = findViewById(R.id.btnSelectDateTo)
        checkboxMonday = findViewById(R.id.checkboxMonday)
        checkboxTuesday = findViewById(R.id.checkboxTuesday)
        checkboxWednesday = findViewById(R.id.checkboxWednesday)
        checkboxThursday = findViewById(R.id.checkboxThursday)
        checkboxFriday = findViewById(R.id.checkboxFriday)
        checkboxSaturday = findViewById(R.id.checkboxSaturday)
        checkboxSunday = findViewById(R.id.checkboxSunday)
        spinnerFrequency = findViewById(R.id.spinnerFrequency)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        eventInfoCard = findViewById(R.id.eventInfoCard)
        eventInfoText = findViewById(R.id.eventInfoText)
    }

    private fun setupEventInfo() {
        if (eventDocumentId != null && eventTitle != null) {
            eventInfoCard.visibility = View.VISIBLE
            eventInfoText.text = "Veranstaltung: $eventTitle"
        } else {
            eventInfoCard.visibility = View.GONE
        }
    }

    private fun setupDateTypeSpinner() {
        val dateTypes = if (eventDocumentId != null) {
            // When creating for an event, offer "An Veranstaltungstagen" as first option
            arrayOf("An Veranstaltungstagen", "Spezifisches Datum", "Zeitraum", "Wiederkehrend")
        } else {
            arrayOf("Spezifisches Datum", "Zeitraum", "Wiederkehrend")
        }
        val adapter = ArrayAdapter(this, R.layout.spinner_item, dateTypes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerDateType.adapter = adapter

        spinnerDateType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                layoutFixedDate.visibility = View.GONE
                layoutDateRange.visibility = View.GONE
                layoutRecurring.visibility = View.GONE
                layoutEventDays.visibility = View.GONE

                if (eventDocumentId != null) {
                    // Event mode: first option is "An Veranstaltungstagen"
                    when (position) {
                        0 -> layoutEventDays.visibility = View.VISIBLE
                        1 -> layoutFixedDate.visibility = View.VISIBLE
                        2 -> layoutDateRange.visibility = View.VISIBLE
                        3 -> layoutRecurring.visibility = View.VISIBLE
                    }
                } else {
                    // Standard mode
                    when (position) {
                        0 -> layoutFixedDate.visibility = View.VISIBLE
                        1 -> layoutDateRange.visibility = View.VISIBLE
                        2 -> layoutRecurring.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFrequencySpinner() {
        val frequencies = arrayOf("Wöchentlich", "Alle zwei Wochen", "Monatlich")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, frequencies)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerFrequency.adapter = adapter
    }

    private fun setupListeners() {
        btnSelectDate.setOnClickListener {
            showDateTimePicker { calendar ->
                selectedDate = calendar
                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
                btnSelectDate.text = format.format(calendar.time)
            }
        }

        btnSelectDateFrom.setOnClickListener {
            showDatePicker { calendar ->
                selectedDateFrom = calendar
                val format = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                btnSelectDateFrom.text = format.format(calendar.time)
            }
        }

        btnSelectDateTo.setOnClickListener {
            showDatePicker { calendar ->
                selectedDateTo = calendar
                val format = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                btnSelectDateTo.text = format.format(calendar.time)
            }
        }

        btnSave.setOnClickListener {
            saveMeeting()
        }
    }

    private fun showDateTimePicker(onSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, R.style.Theme_Meply_DatePicker, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                onSelected(calendar)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showDatePicker(onSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, R.style.Theme_Meply_DatePicker, { _, year, month, day ->
            calendar.set(year, month, day)
            onSelected(calendar)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveMeeting() {
        val title = inputTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Bitte gib einen Titel ein", Toast.LENGTH_SHORT).show()
            return
        }

        val description = inputDescription.text.toString().trim()
        val dateTypePosition = spinnerDateType.selectedItemPosition

        val dateInfo: Pair<MeetingDatesRequest, String?> = if (eventDocumentId != null) {
            // Event mode
            when (dateTypePosition) {
                0 -> { // Event days
                    val datesObj = MeetingDatesRequest(
                        type = "eventDays",
                        value = emptyMap()
                    )
                    Pair(datesObj, null)
                }
                1 -> buildFixedDateInfo() ?: return
                2 -> buildDateRangeInfo() ?: return
                3 -> buildRecurringInfo() ?: return
                else -> return
            }
        } else {
            // Standard mode
            when (dateTypePosition) {
                0 -> buildFixedDateInfo() ?: return
                1 -> buildDateRangeInfo() ?: return
                2 -> buildRecurringInfo() ?: return
                else -> return
            }
        }

        val (dates, filterDate) = dateInfo

        val profileId = AuthManager.getProfileDocumentId(this)
        if (profileId == null) {
            Toast.makeText(this, "Nicht angemeldet", Toast.LENGTH_SHORT).show()
            return
        }

        val meetingData = MeetingDataRequest(
            title = title,
            description = description.ifEmpty { null },
            dates = dates,
            date = filterDate,
            author = profileId,
            event = eventDocumentId // This will be null if not creating for an event
        )

        createMeeting(meetingData)
    }

    private fun buildFixedDateInfo(): Pair<MeetingDatesRequest, String?>? {
        if (selectedDate == null) {
            Toast.makeText(this, "Bitte wähle ein Datum aus", Toast.LENGTH_SHORT).show()
            return null
        }
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val dateStr = format.format(selectedDate!!.time)

        val datesObj = MeetingDatesRequest(
            type = "fixed",
            value = mapOf("date" to dateStr)
        )
        return Pair(datesObj, dateStr)
    }

    private fun buildDateRangeInfo(): Pair<MeetingDatesRequest, String?>? {
        if (selectedDateFrom == null || selectedDateTo == null) {
            Toast.makeText(this, "Bitte wähle Start- und Enddatum aus", Toast.LENGTH_SHORT).show()
            return null
        }
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
        val startDate = format.format(selectedDateFrom!!.time)
        val endDate = format.format(selectedDateTo!!.time)

        val datesObj = MeetingDatesRequest(
            type = "range",
            value = mapOf("start" to startDate, "end" to endDate)
        )
        return Pair(datesObj, endDate)
    }

    private fun buildRecurringInfo(): Pair<MeetingDatesRequest, String?>? {
        val selectedDays = mutableListOf<String>()
        if (checkboxMonday.isChecked) selectedDays.add("monday")
        if (checkboxTuesday.isChecked) selectedDays.add("tuesday")
        if (checkboxWednesday.isChecked) selectedDays.add("wednesday")
        if (checkboxThursday.isChecked) selectedDays.add("thursday")
        if (checkboxFriday.isChecked) selectedDays.add("friday")
        if (checkboxSaturday.isChecked) selectedDays.add("saturday")
        if (checkboxSunday.isChecked) selectedDays.add("sunday")

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Bitte wähle mindestens einen Tag aus", Toast.LENGTH_SHORT).show()
            return null
        }

        val frequency = when (spinnerFrequency.selectedItemPosition) {
            0 -> "weekly"
            1 -> "biweekly"
            2 -> "monthly"
            else -> "weekly"
        }

        val datesObj = MeetingDatesRequest(
            type = "recurring",
            value = mapOf("days" to selectedDays, "frequency" to frequency)
        )
        return Pair(datesObj, null)
    }

    private fun createMeeting(meetingData: MeetingDataRequest) {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        val request = CreateMeetingRequest(data = meetingData)

        ApiClient.retrofit.createMeeting(request).enqueue(object : Callback<MeetingResponse> {
            override fun onResponse(call: Call<MeetingResponse>, response: Response<MeetingResponse>) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true

                if (response.isSuccessful) {
                    Toast.makeText(this@CreateMeetingActivity, "Gesuch erstellt", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@CreateMeetingActivity, "Fehler beim Erstellen: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MeetingResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this@CreateMeetingActivity, "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val EXTRA_EVENT_DOCUMENT_ID = "event_document_id"
        private const val EXTRA_EVENT_TITLE = "event_title"

        /**
         * Start CreateMeetingActivity without event association (standalone meeting)
         */
        fun start(context: Context) {
            val intent = Intent(context, CreateMeetingActivity::class.java)
            context.startActivity(intent)
        }

        /**
         * Start CreateMeetingActivity for a specific event
         * @param eventDocumentId The document ID of the event
         * @param eventTitle The title of the event (for display)
         */
        fun startForEvent(context: Context, eventDocumentId: String, eventTitle: String) {
            val intent = Intent(context, CreateMeetingActivity::class.java).apply {
                putExtra(EXTRA_EVENT_DOCUMENT_ID, eventDocumentId)
                putExtra(EXTRA_EVENT_TITLE, eventTitle)
            }
            context.startActivity(intent)
        }
    }
}
