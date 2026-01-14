package de.meply.meply

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import de.meply.meply.data.meeting.Meeting
import de.meply.meply.data.meeting.MeetingsResponse
import de.meply.meply.network.ApiClient
import de.meply.meply.auth.AuthManager
import de.meply.meply.utils.AvatarUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class GesucheFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var meetingsContainer: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var btnCreateMeeting: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gesuche, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        meetingsContainer = view.findViewById(R.id.meetingsContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        btnCreateMeeting = view.findViewById(R.id.btnCreateMeeting)

        swipeRefresh.setOnRefreshListener {
            loadMeetings()
        }

        btnCreateMeeting.setOnClickListener {
            openCreateMeeting()
        }

        loadMeetings()
    }

    private fun loadMeetings() {
        progressBar.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
        meetingsContainer.removeAllViews()

        val userId = AuthManager.getUserDocumentId(requireContext())
        if (userId == null) {
            Toast.makeText(requireContext(), "Nicht angemeldet", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            return
        }

        ApiClient.retrofit.getUserMeetings(
            authorDocumentId = userId,
            dateIsNull = true,
            dateIsGte = "1984-05-05" // Magic date to show all user meetings
        ).enqueue(object : Callback<MeetingsResponse> {
            override fun onResponse(call: Call<MeetingsResponse>, response: Response<MeetingsResponse>) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val meetings = response.body()?.data ?: emptyList()
                    if (meetings.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                    } else {
                        displayMeetings(meetings)
                    }
                } else {
                    Toast.makeText(requireContext(), "Fehler beim Laden der Gesuche", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MeetingsResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayMeetings(meetings: List<Meeting>) {
        meetings.forEach { meeting ->
            val meetingView = createMeetingCard(meeting)
            meetingsContainer.addView(meetingView)
        }
    }

    private fun createMeetingCard(meeting: Meeting): View {
        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            radius = 8f
            cardElevation = 2f
            setCardBackgroundColor(resources.getColor(R.color.background_card, null))
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Header with avatar and username
        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val avatar = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                rightMargin = 12
            }
        }

        val avatarUrl = meeting.author?.avatar?.firstOrNull()?.url
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load("${ApiClient.STRAPI_IMAGE_BASE}$avatarUrl")
                .circleCrop()
                .into(avatar)
        } else {
            val userId = meeting.author?.documentId ?: "default"
            Glide.with(this)
                .load(AvatarUtils.getDefaultAvatarUrl(userId))
                .circleCrop()
                .into(avatar)
        }

        headerLayout.addView(avatar)

        val userInfoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val usernameText = TextView(requireContext()).apply {
            text = meeting.author?.username ?: "Unbekannt"
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        userInfoLayout.addView(usernameText)

        val cityText = TextView(requireContext()).apply {
            text = meeting.author?.city ?: ""
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
        }
        userInfoLayout.addView(cityText)

        headerLayout.addView(userInfoLayout)
        contentLayout.addView(headerLayout)

        // Location/Event info
        val locationInfo = when {
            meeting.location != null -> meeting.location.titel ?: meeting.location.ort
            meeting.event != null -> meeting.event.title
            else -> "Freies Gesuch"
        }

        val locationText = TextView(requireContext()).apply {
            text = locationInfo
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(0, 8, 0, 0)
        }
        contentLayout.addView(locationText)

        // Title
        val titleText = TextView(requireContext()).apply {
            text = meeting.title ?: "Kein Titel"
            textSize = 18f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 12, 0, 0)
        }
        contentLayout.addView(titleText)

        // Date info
        val dateText = TextView(requireContext()).apply {
            text = formatMeetingDate(meeting)
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            setPadding(0, 8, 0, 0)
        }
        contentLayout.addView(dateText)

        // Description
        if (!meeting.description.isNullOrEmpty()) {
            val descriptionText = TextView(requireContext()).apply {
                text = meeting.description
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_primary, null))
                setPadding(0, 8, 0, 0)
            }
            contentLayout.addView(descriptionText)
        }

        // Action buttons
        val actionsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 0)
        }

        val deleteButton = MaterialButton(requireContext()).apply {
            text = "Löschen"
            setIconResource(android.R.drawable.ic_menu_delete)
            setOnClickListener {
                deleteMeeting(meeting)
            }
        }
        actionsLayout.addView(deleteButton)

        contentLayout.addView(actionsLayout)
        cardView.addView(contentLayout)

        return cardView
    }

    private fun formatMeetingDate(meeting: Meeting): String {
        val dates = meeting.dates
        return when (dates?.type) {
            "fixed" -> {
                val date = (dates.value?.get("date") as? String) ?: ""
                "Termin: ${formatDate(date)}"
            }
            "range" -> {
                val start = (dates.value?.get("start") as? String) ?: ""
                val end = (dates.value?.get("end") as? String) ?: ""
                "Zeitraum: ${formatDate(start)} - ${formatDate(end)}"
            }
            "recurring" -> {
                val frequency = (dates.value?.get("frequency") as? String) ?: ""
                val days = (dates.value?.get("days") as? List<*>)?.joinToString(", ") ?: ""
                "Wiederkehrend: $days ($frequency)"
            }
            "eventDays" -> {
                val days = (dates.value?.get("days") as? List<*>)?.joinToString(", ") { formatDate(it.toString()) } ?: ""
                "Event-Tage: $days"
            }
            else -> meeting.date?.let { "Datum: ${formatDate(it)}" } ?: "Kein Datum"
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.GERMAN)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateStr)
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            // Try simple date format
            try {
                val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
                val date = simpleFormat.parse(dateStr)
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                date?.let { outputFormat.format(it) } ?: dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }

    private fun deleteMeeting(meeting: Meeting) {
        val documentId = meeting.documentId ?: return

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Gesuch löschen")
            .setMessage("Möchtest du dieses Gesuch wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                performDelete(documentId)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun performDelete(documentId: String) {
        progressBar.visibility = View.VISIBLE

        ApiClient.retrofit.deleteMeeting(documentId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Gesuch gelöscht", Toast.LENGTH_SHORT).show()
                    loadMeetings()
                } else {
                    Toast.makeText(requireContext(), "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openCreateMeeting() {
        val intent = Intent(requireContext(), CreateMeetingActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadMeetings()
    }
}
