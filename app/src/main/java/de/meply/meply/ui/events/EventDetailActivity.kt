package de.meply.meply.ui.events

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import de.meply.meply.BaseDetailActivity
import de.meply.meply.R
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.data.feed.HasLikedResponse
import de.meply.meply.data.feed.LikeToggleRequest
import de.meply.meply.data.feed.LikeToggleResponse
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
import android.widget.Button
import com.google.android.material.appbar.MaterialToolbar

class EventDetailActivity : BaseDetailActivity() {

    private var eventSlugOrId: String? = null
    private var currentEventTitle: String? = null
    private var currentEventStartDate: String? = null
    private var currentEventEndDate: String? = null

    // Header Card
    private lateinit var titleTextView: TextView
    private lateinit var likesContainer: LinearLayout
    private lateinit var likeIcon: ImageView
    private lateinit var likeCount: TextView
    private lateinit var headerMeetingCount: TextView

    // Info Card
    private lateinit var dateTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var organizerTextView: TextView
    private lateinit var urlContainer: LinearLayout
    private lateinit var urlDivider: View
    private lateinit var urlTextView: TextView

    // Description Card
    private lateinit var descriptionCard: MaterialCardView
    private lateinit var descriptionTextView: TextView

    // Progress
    private lateinit var progressBar: ProgressBar

    // Meetings UI elements
    private lateinit var meetingsProgressBar: ProgressBar
    private lateinit var meetingsErrorCard: MaterialCardView
    private lateinit var meetingsErrorTextView: TextView
    private lateinit var noMeetingsCard: MaterialCardView
    private lateinit var noMeetingsTextView: TextView
    private lateinit var meetingsRecycler: RecyclerView
    private lateinit var meetingsAdapter: MeetingsAdapter

    private var currentEventDocumentId: String? = null
    private var currentLikeCount: Int = 0
    private var isLiked: Boolean = false
    private var likeStateChanged: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // Header Card
        titleTextView = findViewById(R.id.detail_event_title)
        likesContainer = findViewById(R.id.likesContainer)
        likeIcon = findViewById(R.id.likeIcon)
        likeCount = findViewById(R.id.likeCount)
        headerMeetingCount = findViewById(R.id.headerMeetingCount)

        // Info Card
        dateTextView = findViewById(R.id.detail_event_date)
        locationTextView = findViewById(R.id.detail_event_location)
        organizerTextView = findViewById(R.id.detail_event_organizer)
        urlContainer = findViewById(R.id.urlContainer)
        urlDivider = findViewById(R.id.urlDivider)
        urlTextView = findViewById(R.id.detail_event_url)

        // Description Card
        descriptionCard = findViewById(R.id.descriptionCard)
        descriptionTextView = findViewById(R.id.detail_event_description)

        // Progress
        progressBar = findViewById(R.id.detail_event_progress_bar)

        // Initialize meetings UI elements
        meetingsProgressBar = findViewById(R.id.detail_meetings_progress_bar)
        meetingsErrorCard = findViewById(R.id.meetings_error_card)
        meetingsErrorTextView = findViewById(R.id.detail_meetings_error_textview)
        noMeetingsCard = findViewById(R.id.no_meetings_card)
        noMeetingsTextView = findViewById(R.id.detail_no_meetings_textview)
        meetingsRecycler = findViewById(R.id.detail_meetings_recycler)

        // Setup meetings RecyclerView and adapter
        meetingsAdapter = MeetingsAdapter(
            onContactClick = { meeting -> onContactMeeting(meeting) },
            onAuthorClick = { userSlug -> openUserProfile(userSlug) }
        )
        meetingsRecycler.layoutManager = LinearLayoutManager(this)
        meetingsRecycler.adapter = meetingsAdapter

        // Setup like button click
        likesContainer.setOnClickListener {
            toggleLike()
        }

        // Setup toolbar with back button and "Gesuch +" button
        setupEventDetailToolbar()

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

    private fun setupEventDetailToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val btnCreateMeeting = findViewById<Button>(R.id.btnCreateMeeting)

        // Back button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // "Gesuch +" button click
        btnCreateMeeting.setOnClickListener {
            showCreateMeetingBottomSheet()
        }
    }

    private fun showCreateMeetingBottomSheet() {
        val docId = currentEventDocumentId
        val title = currentEventTitle

        if (docId == null || title == null) {
            Toast.makeText(this, "Event-Daten noch nicht geladen", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = CreateMeetingBottomSheet.newInstance(
            eventDocumentId = docId,
            eventTitle = title,
            eventStartDate = currentEventStartDate,
            eventEndDate = currentEventEndDate
        )

        bottomSheet.setOnMeetingCreatedListener {
            // Refresh meetings list when a new meeting is created
            fetchMeetingsForEvent(docId)
        }

        bottomSheet.show(supportFragmentManager, "createMeeting")
    }

    private fun toggleLike() {
        val documentId = currentEventDocumentId ?: return

        // Store current state before API call
        val wasLiked = isLiked
        val oldLikeCount = currentLikeCount

        val request = LikeToggleRequest(
            targetDocumentId = documentId,
            targetType = "event"
        )

        ApiClient.retrofit.toggleLike(request).enqueue(object : Callback<LikeToggleResponse> {
            override fun onResponse(
                call: Call<LikeToggleResponse>,
                response: Response<LikeToggleResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    val newIsLiked = result?.status == "liked"

                    // Calculate new like count locally since API doesn't return it
                    currentLikeCount = if (newIsLiked && !wasLiked) {
                        oldLikeCount + 1
                    } else if (!newIsLiked && wasLiked) {
                        maxOf(0, oldLikeCount - 1)
                    } else {
                        oldLikeCount
                    }

                    isLiked = newIsLiked
                    likeStateChanged = true
                    updateLikeUI()
                    updateResultForCaller()
                    Log.d("EventDetailActivity", "Like toggled: ${result?.status}, wasLiked=$wasLiked, isLiked=$isLiked, oldCount=$oldLikeCount, newCount=$currentLikeCount")
                } else {
                    Log.e("EventDetailActivity", "Failed to toggle like: ${response.code()}")
                    Toast.makeText(this@EventDetailActivity, "Fehler beim Liken", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LikeToggleResponse>, t: Throwable) {
                Log.e("EventDetailActivity", "Error toggling like", t)
                Toast.makeText(this@EventDetailActivity, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateLikeUI() {
        likeCount.text = currentLikeCount.toString()
        // Use single filled star, change color only
        // White for liked (visible on yellow footer) and dark for not liked
        likeIcon.setImageResource(R.drawable.ic_star_filled)
        if (isLiked) {
            likeIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))
        } else {
            likeIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_on_light))
        }
    }

    private fun updateResultForCaller() {
        if (likeStateChanged && currentEventDocumentId != null) {
            val resultIntent = Intent().apply {
                putExtra(RESULT_DOCUMENT_ID, currentEventDocumentId)
                putExtra(RESULT_LIKE_COUNT, currentLikeCount)
                putExtra(RESULT_IS_LIKED, isLiked)
            }
            setResult(RESULT_OK, resultIntent)
        }
    }

    /**
     * Fetch liked status for this event
     */
    private fun fetchLikedStatus(documentId: String) {
        ApiClient.retrofit.hasLiked(listOf(documentId)).enqueue(object : Callback<HasLikedResponse> {
            override fun onResponse(
                call: Call<HasLikedResponse>,
                response: Response<HasLikedResponse>
            ) {
                if (response.isSuccessful) {
                    val likedIds = response.body()?.liked ?: emptyList()
                    val wasLiked = isLiked
                    isLiked = likedIds.contains(documentId)

                    // Only update UI if status changed
                    if (wasLiked != isLiked) {
                        updateLikeUI()
                        Log.d("EventDetailActivity", "Liked status updated: $isLiked")
                    }
                } else {
                    Log.e("EventDetailActivity", "Failed to fetch liked status: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<HasLikedResponse>, t: Throwable) {
                Log.e("EventDetailActivity", "Error fetching liked status", t)
            }
        })
    }

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
                    } else if (meetingsResponse?.data == null && meetingsResponse?.meta != null) {
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
            meetingsErrorCard.visibility = View.GONE
            noMeetingsCard.visibility = View.GONE
            meetingsRecycler.visibility = View.GONE
        }
        Log.d("EventDetailActivity", "showMeetingsLoading: $isLoading")
    }

    private fun displayMeetings(meetings: List<MeetingData>) {
        // Update header meeting count
        val count = meetings.size
        headerMeetingCount.text = if (count == 1) "1 Gesuch" else "$count Gesuche"

        if (meetings.isEmpty()) {
            Log.i("EventDetailActivity", "No meetings to display.")
            noMeetingsCard.visibility = View.VISIBLE
            meetingsRecycler.visibility = View.GONE
            meetingsErrorCard.visibility = View.GONE
        } else {
            Log.i("EventDetailActivity", "Displaying ${meetings.size} meetings.")
            noMeetingsCard.visibility = View.GONE
            meetingsRecycler.visibility = View.VISIBLE
            meetingsErrorCard.visibility = View.GONE
            meetingsAdapter.submitList(meetings)
        }
    }

    private fun showMeetingsError(message: String) {
        meetingsErrorTextView.text = message
        meetingsErrorCard.visibility = View.VISIBLE
        noMeetingsCard.visibility = View.GONE
        meetingsRecycler.visibility = View.GONE
        Log.e("EventDetailActivity", "showMeetingsError: $message")
    }

    private fun fetchEventDetails(slug: String) {
        showLoading(true)
        ApiClient.retrofit.getEventBySlug(slug = slug)
            .enqueue(object : Callback<StrapiListResponse<FlatEventData>> {

                override fun onResponse(
                    call: Call<StrapiListResponse<FlatEventData>>,
                    response: Response<StrapiListResponse<FlatEventData>>
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

                override fun onFailure(
                    call: Call<StrapiListResponse<FlatEventData>>,
                    t: Throwable
                ) {
                    showLoading(false)
                    Log.e("EventDetailActivity", "Network Error: ${t.message}", t)
                    showError("Netzwerkfehler: ${t.message}")
                }
            })
    }

    private fun displayEventDetails(eventData: FlatEventData) {
        // Title
        val title = eventData.title ?: "Ohne Titel"
        titleTextView.text = title
        currentEventTitle = title

        // Store event dates for meeting creation
        currentEventStartDate = eventData.startDate
        currentEventEndDate = eventData.endDate

        // Like count and state
        currentLikeCount = eventData.likes ?: 0
        isLiked = eventData.liked
        updateLikeUI()

        // Fetch liked status if we have a document ID
        eventData.documentId?.let { docId ->
            fetchLikedStatus(docId)
        }

        // Date
        dateTextView.text = formatEventDate(eventData)

        // Location
        val addressParts = mutableListOf<String>()
        val streetLine = listOfNotNull(eventData.street, eventData.streetNumber).joinToString(" ").trim()
        if (streetLine.isNotBlank()) {
            addressParts.add(streetLine)
        }
        val cityLine = listOfNotNull(eventData.zip, eventData.city).joinToString(" ").trim()
        if (cityLine.isNotBlank()) {
            addressParts.add(cityLine)
        }
        if (!eventData.country.isNullOrBlank() && eventData.country != "Deutschland") {
            addressParts.add(eventData.country!!)
        }

        if (addressParts.isNotEmpty()) {
            locationTextView.text = addressParts.joinToString(", ")
        } else if (!eventData.city.isNullOrBlank()) {
            locationTextView.text = eventData.city
        } else {
            locationTextView.text = "Keine Angabe"
        }

        // Organizer (currently not in API response, show generic text)
        organizerTextView.text = "Veranstalter"

        // URL
        if (!eventData.url.isNullOrBlank()) {
            urlDivider.visibility = View.VISIBLE
            urlContainer.visibility = View.VISIBLE
            urlTextView.text = eventData.url
            urlTextView.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(eventData.url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Kann Link nicht öffnen", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            urlDivider.visibility = View.GONE
            urlContainer.visibility = View.GONE
        }

        // Description
        val description = eventData.description?.replace("<br />", "\n")?.replace("<br>", "\n")
        if (!description.isNullOrBlank()) {
            descriptionCard.visibility = View.VISIBLE
            descriptionTextView.text = description
        } else {
            descriptionCard.visibility = View.GONE
        }

        // Fetch meetings
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
                    dateStr += " - ${outputDateFormat.format(endDateObj)}"
                }
                if (!eventData.startTime.isNullOrBlank()) {
                    val startTimeObj = timeFormat.parse(eventData.startTime!!)
                    dateStr += " | ${outputTimeFormat.format(startTimeObj)}"
                    if (!eventData.endTime.isNullOrBlank() && eventData.endTime != eventData.startTime) {
                        val endTimeObj = timeFormat.parse(eventData.endTime!!)
                        dateStr += " - ${outputTimeFormat.format(endTimeObj)}"
                    }
                }
                return dateStr
            } catch (e: Exception) {
                Log.e("EventDetailActivity", "Error formatting date for eventData: $eventData -> ${e.message}", e)
                return eventData.datePlaceholder ?: "Termin folgt"
            }
        } else {
            return eventData.datePlaceholder ?: "Termin folgt"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        titleTextView.text = message
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
        messageInput.setPadding(48, 32, 48, 32)

        MaterialAlertDialogBuilder(this, R.style.Theme_Meply_AlertDialog)
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

    private fun openUserProfile(userSlug: String) {
        de.meply.meply.ui.profile.UserProfileActivity.start(this, userSlug)
    }

    companion object {
        private const val EXTRA_EVENT_SLUG_OR_ID = "event_slug_or_id"
        const val RESULT_DOCUMENT_ID = "result_document_id"
        const val RESULT_LIKE_COUNT = "result_like_count"
        const val RESULT_IS_LIKED = "result_is_liked"

        fun start(context: Context, eventSlugOrId: String) {
            val intent = Intent(context, EventDetailActivity::class.java).apply {
                putExtra(EXTRA_EVENT_SLUG_OR_ID, eventSlugOrId)
            }
            context.startActivity(intent)
        }

        fun createIntent(context: Context, eventSlugOrId: String): Intent {
            return Intent(context, EventDetailActivity::class.java).apply {
                putExtra(EXTRA_EVENT_SLUG_OR_ID, eventSlugOrId)
            }
        }
    }
}
