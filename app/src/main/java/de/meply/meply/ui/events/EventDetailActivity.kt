package de.meply.meply.ui.events

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import de.meply.meply.BaseDetailActivity
import de.meply.meply.R
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.data.meetings.MeetingData
import de.meply.meply.data.messages.CreateConversationRequest
import de.meply.meply.data.messages.SendMessageResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.meply.meply.data.events.FlatEventData

class EventDetailActivity : BaseDetailActivity() {

    private var eventSlugOrId: String? = null
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var organizerTextView: TextView
    private lateinit var progressBar: ProgressBar

    // Meetings UI elements
    private lateinit var meetingsProgressBar: ProgressBar
    private lateinit var meetingsErrorTextView: TextView
    private lateinit var noMeetingsTextView: TextView
    private lateinit var meetingsRecycler: RecyclerView
    private lateinit var meetingsAdapter: MeetingsAdapter

    private var currentEventDocumentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        titleTextView = findViewById(R.id.detail_event_title)
        descriptionTextView = findViewById(R.id.detail_event_description)
        dateTextView = findViewById(R.id.detail_event_date)
        locationTextView = findViewById(R.id.detail_event_location)
        organizerTextView = findViewById(R.id.detail_event_organizer)
        progressBar = findViewById(R.id.detail_event_progress_bar)

        // Initialize meetings UI elements
        meetingsProgressBar = findViewById(R.id.detail_meetings_progress_bar)
        meetingsErrorTextView = findViewById(R.id.detail_meetings_error_textview)
        noMeetingsTextView = findViewById(R.id.detail_no_meetings_textview)
        meetingsRecycler = findViewById(R.id.detail_meetings_recycler)

        // Setup meetings RecyclerView and adapter
        meetingsAdapter = MeetingsAdapter { meeting -> onContactMeeting(meeting) }
        meetingsRecycler.layoutManager = LinearLayoutManager(this)
        meetingsRecycler.adapter = meetingsAdapter

        // Setup toolbar with back button and user menu
        setupDetailToolbar()

        eventSlugOrId = intent.getStringExtra(EXTRA_EVENT_SLUG_OR_ID)

        if (eventSlugOrId == null) {
            Log.e("EventDetailActivity", "Error: No event slug or ID passed to activity.")
            Toast.makeText(this, "Fehler: Event nicht gefunden.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d("EventDetailActivity", "Received slug or ID: $eventSlugOrId")
        fetchEventDetails(eventSlugOrId!!)
    }

    // HINZUGEFÜGT: Funktion zum Abrufen des heutigen Datums
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)
        return sdf.format(Date())
    }

    private fun fetchMeetingsForEvent(eventDocId: String) {
        showMeetingsLoading(true)
        val today = getTodayDateString()

        ApiClient.retrofit.getMeetingsForEvent(
            eventDocumentId = eventDocId,
            dateIsGte = today
        ).enqueue(object : Callback<StrapiListResponse<MeetingData>> {
            override fun onResponse(
                call: Call<StrapiListResponse<MeetingData>>,
                response: Response<StrapiListResponse<MeetingData>>
            ) {
                showMeetingsLoading(false)
                if (response.isSuccessful) {
                    val responseBodyString = response.body()?.let { Gson().toJson(it) } ?: response.errorBody()?.string()
                    Log.d("EventDetailActivity_Meetings", "Meetings Raw JSON Response: $responseBodyString")

                    val meetingsResponse = response.body()
                    if (meetingsResponse != null && meetingsResponse.data != null) {
                        Log.i("EventDetailActivity_Meetings", "Successfully fetched ${meetingsResponse.data.size} meetings.")
                        displayMeetings(meetingsResponse.data)
                    } else if (meetingsResponse?.data == null && meetingsResponse?.meta != null) { // meetingsResponse.meta statt response.meta
                        Log.i("EventDetailActivity_Meetings", "No meetings found (data is null, but meta present).")
                        displayMeetings(emptyList())
                    } else {
                        Log.w("EventDetailActivity_Meetings", "Meetings response body or data is null, even if successful.")
                        showMeetingsError("Fehler beim Verarbeiten der Meeting-Daten.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("EventDetailActivity_Meetings", "API Error fetching meetings: ${response.code()} - ${response.message()}. Error Body: $errorBody")
                    showMeetingsError("Fehler ${response.code()} beim Laden der Spieltreffen.")
                }
            }

            override fun onFailure(call: Call<StrapiListResponse<MeetingData>>, t: Throwable) {
                showMeetingsLoading(false)
                Log.e("EventDetailActivity_Meetings", "Network Error fetching meetings: ${t.message}", t)
                showMeetingsError("Netzwerkfehler beim Laden der Spieltreffen.")
            }
        })
    }

    private fun showMeetingsLoading(isLoading: Boolean) {
        meetingsProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            meetingsErrorTextView.visibility = View.GONE
            noMeetingsTextView.visibility = View.GONE
            meetingsRecycler.visibility = View.GONE
        }
        Log.d("EventDetailActivity", "showMeetingsLoading: $isLoading")
    }

    private fun displayMeetings(meetings: List<MeetingData>) {
        if (meetings.isEmpty()) {
            Log.i("EventDetailActivity", "No meetings to display.")
            noMeetingsTextView.visibility = View.VISIBLE
            meetingsRecycler.visibility = View.GONE
            meetingsErrorTextView.visibility = View.GONE
        } else {
            Log.i("EventDetailActivity", "Displaying ${meetings.size} meetings.")
            noMeetingsTextView.visibility = View.GONE
            meetingsRecycler.visibility = View.VISIBLE
            meetingsErrorTextView.visibility = View.GONE
            meetingsAdapter.submitList(meetings)
        }
    }

    private fun showMeetingsError(message: String) {
        meetingsErrorTextView.text = message
        meetingsErrorTextView.visibility = View.VISIBLE
        noMeetingsTextView.visibility = View.GONE
        meetingsRecycler.visibility = View.GONE
        Log.e("EventDetailActivity", "showMeetingsError: $message")
    }

    private fun fetchEventDetails(slug: String) {
        showLoading(true)
        ApiClient.retrofit.getEventBySlug(slug = slug)
            .enqueue(object : Callback<StrapiListResponse<FlatEventData>> { // KORREKTER TYP hier

                // KORREKTE onResponse SIGNATUR
                override fun onResponse(
                    call: Call<StrapiListResponse<FlatEventData>>, // Muss mit dem Typ im Callback übereinstimmen
                    response: Response<StrapiListResponse<FlatEventData>> // Muss mit dem Typ im Callback übereinstimmen
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val flatEventData = response.body()?.data?.firstOrNull()
                        if (flatEventData != null) {
                            displayEventDetails(flatEventData)
                        } else {
                            Log.e("EventDetailActivity", "Event not found for slug: $slug")
                            showError("Event nicht gefunden.")
                        }
                    } else {
                        Log.e("EventDetailActivity", "API Error: ${response.code()} - ${response.message()}")
                        showError("Fehler beim Laden der Event-Details (${response.code()}).")
                    }
                }

                // KORREKTE onFailure SIGNATUR
                override fun onFailure(
                    call: Call<StrapiListResponse<FlatEventData>>, // Muss mit dem Typ im Callback übereinstimmen
                    t: Throwable
                ) {
                    showLoading(false)
                    Log.e("EventDetailActivity", "Network Error: ${t.message}", t)
                    showError("Netzwerkfehler: ${t.message}")
                }
            })
    }

    // ÜBERARBEITET: Ruft jetzt auch fetchMeetingsForEvent auf
    private fun displayEventDetails(eventData: FlatEventData) {
        titleTextView.text = eventData.title ?: "Ohne Titel"
        descriptionTextView.text = eventData.description?.replace("<br />", "\n") ?: "Keine Beschreibung vorhanden."
        dateTextView.text = formatEventDate(eventData)

        val addressParts = mutableListOf<String>()
        val streetLine = listOfNotNull(eventData.street, eventData.streetNumber).joinToString(" ").trim()
        if (streetLine.isNotBlank()) {
            addressParts.add(streetLine)
        }
        val cityLine = listOfNotNull(eventData.zip, eventData.city).joinToString(" ").trim()
        if (cityLine.isNotBlank()) {
            addressParts.add(cityLine)
        }
        if (!eventData.country.isNullOrBlank()) {
            addressParts.add(eventData.country!!)
        }

        if (addressParts.isNotEmpty()) {
            locationTextView.text = "Ort: ${addressParts.joinToString(", ")}"
        } else if (!eventData.city.isNullOrBlank()){
            locationTextView.text = "Ort: ${eventData.city}"
        } else {
            locationTextView.text = "Ort: Keine Angabe"
        }
        organizerTextView.text = "Veranstalter: Keine Angabe" // Bleibt vorerst so

        // NEU: currentEventDocumentId setzen und Meetings abrufen
        // Stellen Sie sicher, dass FlatEventData ein Feld 'documentId' hat
        eventData.documentId?.let { docId ->
            if (docId.isNotBlank()) {
                currentEventDocumentId = docId
                fetchMeetingsForEvent(docId)
            } else {
                Log.w("EventDetailActivity", "Event documentId from FlatEventData is blank, cannot fetch meetings.")
                showMeetingsError("Event-ID für Spieltreffen fehlt.")
            }
        } ?: run {
            Log.e("EventDetailActivity", "Event documentId is null in FlatEventData, cannot fetch meetings.")
            showMeetingsError("Event-ID für Spieltreffen nicht gefunden.")
        }
    }

    private fun formatEventDate(eventData: FlatEventData): String {
        // ... (Ihre bestehende formatEventDate-Logik - unverändert gelassen)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
        val outputDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.GERMANY)

        if (eventData.fixedDate == true) {
            if (eventData.startDate.isNullOrBlank()) return eventData.datePlaceholder ?: "Termin folgt"
            try {
                val startDateObj = isoFormat.parse(eventData.startDate!!)
                var dateStr = outputDateFormat.format(startDateObj)
                if (!eventData.endDate.isNullOrBlank() && eventData.endDate != eventData.startDate) {
                    val endDateObj = isoFormat.parse(eventData.endDate!!)
                    dateStr += " – ${outputDateFormat.format(endDateObj)}"
                }
                if (!eventData.startTime.isNullOrBlank()) {
                    val startTimeObj = timeFormat.parse(eventData.startTime!!)
                    dateStr += " | ${outputTimeFormat.format(startTimeObj)}"
                    if (!eventData.endTime.isNullOrBlank() && eventData.endTime != eventData.startTime) {
                        val endTimeObj = timeFormat.parse(eventData.endTime!!)
                        dateStr += " – ${outputTimeFormat.format(endTimeObj)}"
                    }
                }
                return dateStr
            } catch (e: Exception) {
                Log.e("EventDetailActivity", "Error formatting date for eventData: $eventData -> ${e.message}", e)
                return eventData.datePlaceholder ?: "Termin folgt (Formatierungsfehler)"
            }
        } else {
            return eventData.datePlaceholder ?: "Termin folgt"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        val contentVisibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        titleTextView.visibility = contentVisibility
        descriptionTextView.visibility = contentVisibility
        dateTextView.visibility = contentVisibility
        locationTextView.visibility = contentVisibility
        organizerTextView.visibility = contentVisibility
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        titleTextView.text = message
        descriptionTextView.visibility = View.GONE
        dateTextView.visibility = View.GONE
        locationTextView.visibility = View.GONE
        organizerTextView.visibility = View.GONE
    }

    private fun onContactMeeting(meeting: MeetingData) {
        val authorDocumentId = meeting.author?.documentId
        val authorName = meeting.author?.username ?: "diesem Nutzer"

        if (authorDocumentId.isNullOrBlank()) {
            Toast.makeText(this, "Autor-ID nicht verfügbar", Toast.LENGTH_SHORT).show()
            Log.e("EventDetailActivity", "Cannot contact meeting author: documentId is null")
            return
        }

        // Create dialog to ask for message
        val messageInput = EditText(this)
        messageInput.hint = "Deine Nachricht"
        messageInput.maxLines = 5

        AlertDialog.Builder(this)
            .setTitle("Nachricht an $authorName")
            .setMessage("Schreibe eine Nachricht zum Spieltreffen \"${meeting.title ?: "Spieltreffen"}\"")
            .setView(messageInput)
            .setPositiveButton("Senden") { _, _ ->
                val messageText = messageInput.text.toString().trim()
                if (messageText.isEmpty()) {
                    Toast.makeText(this, "Bitte eine Nachricht eingeben", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (messageText.length > 500) {
                    Toast.makeText(this, "Nachricht zu lang (max. 500 Zeichen)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendMessageToAuthor(authorDocumentId, messageText, meeting.documentId, authorName)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun sendMessageToAuthor(recipientId: String, message: String, meetingReference: String?, recipientName: String) {
        val request = CreateConversationRequest(
            recipient = recipientId,
            message = message,
            reference = meetingReference
        )

        ApiClient.retrofit.createConversation(request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(
                call: Call<SendMessageResponse>,
                response: Response<SendMessageResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@EventDetailActivity,
                        "Nachricht an $recipientName gesendet!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("EventDetailActivity", "Message sent successfully to $recipientName")
                } else {
                    Toast.makeText(
                        this@EventDetailActivity,
                        "Fehler beim Senden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("EventDetailActivity", "Error sending message: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                Toast.makeText(
                    this@EventDetailActivity,
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("EventDetailActivity", "Network error sending message", t)
            }
        })
    }

    companion object {
        private const val EXTRA_EVENT_SLUG_OR_ID = "event_slug_or_id"
        fun start(context: Context, eventSlugOrId: String) {
            val intent = Intent(context, EventDetailActivity::class.java).apply {
                putExtra(EXTRA_EVENT_SLUG_OR_ID, eventSlugOrId)
            }
            context.startActivity(intent)
        }
    }
}
