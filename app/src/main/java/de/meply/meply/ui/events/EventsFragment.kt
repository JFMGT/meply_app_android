package de.meply.meply.ui.events

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.events.EventItem
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import de.meply.meply.ui.events.EventDetailActivity
class EventsFragment : Fragment() {

    private val DEFAULT_ZIP = "33334"
    private val DEFAULT_RADIUS_KM = 100000.0

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView
    private lateinit var filterSummaryCard: MaterialCardView
    private lateinit var filterSummaryText: TextView
    private lateinit var filterExpandedCard: MaterialCardView
    private lateinit var editZip: TextInputEditText
    private lateinit var editRadius: TextInputEditText
    private lateinit var btnSearch: Button
    private val adapter = EventsAdapter { item -> onEventClicked(item) }

    private var currentZip: String? = null
    private var currentRadius: Double? = null
    private var isFilterExpanded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_events, container, false)
        recycler = v.findViewById(R.id.recyclerEvents)
        progress = v.findViewById(R.id.progress)
        empty    = v.findViewById(R.id.emptyView)
        filterSummaryCard = v.findViewById(R.id.filter_summary_card)
        filterSummaryText = v.findViewById(R.id.filter_summary_text)
        filterExpandedCard = v.findViewById(R.id.filter_expanded_card)
        editZip = v.findViewById(R.id.edit_zip)
        editRadius = v.findViewById(R.id.edit_radius)
        btnSearch = v.findViewById(R.id.btn_search)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Load user preferences from SharedPreferences
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedZip = prefs.getString("postalCode", null)
        val savedRadius = prefs.getInt("searchRadius", 0)

        // Pre-fill filter fields
        if (!savedZip.isNullOrEmpty()) {
            editZip.setText(savedZip)
        }
        if (savedRadius > 0) {
            editRadius.setText(savedRadius.toString())
        }

        // Set up filter toggle
        filterSummaryCard.setOnClickListener {
            toggleFilter()
        }

        // Set up search button
        btnSearch.setOnClickListener {
            performSearch()
        }

        // Set up Enter key handling for both input fields
        editZip.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        editRadius.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Load events with default or saved values
        val initialZip = if (!savedZip.isNullOrEmpty()) savedZip else DEFAULT_ZIP
        val initialRadius = if (savedRadius > 0) savedRadius.toDouble() else DEFAULT_RADIUS_KM
        currentZip = initialZip
        currentRadius = initialRadius
        updateFilterSummary()
        loadEvents(zip = initialZip, radiusKm = initialRadius)

        return v
    }

    private fun toggleFilter() {
        isFilterExpanded = !isFilterExpanded
        if (isFilterExpanded) {
            filterSummaryCard.visibility = View.GONE
            filterExpandedCard.visibility = View.VISIBLE
        } else {
            filterExpandedCard.visibility = View.GONE
            filterSummaryCard.visibility = View.VISIBLE
        }
    }

    private fun updateFilterSummary() {
        val summaryText = if (currentZip != null && currentRadius != null &&
                              currentZip != DEFAULT_ZIP && currentRadius != DEFAULT_RADIUS_KM) {
            "Events ${currentRadius?.toInt()} km rund um ${currentZip}"
        } else {
            "Events in deiner Nähe"
        }
        filterSummaryText.text = summaryText
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun performSearch() {
        val zip = editZip.text.toString().trim()
        val radiusStr = editRadius.text.toString().trim()

        if (zip.isEmpty() || zip.length != 5) {
            Toast.makeText(requireContext(), "Bitte gib eine 5-stellige PLZ ein", Toast.LENGTH_SHORT).show()
            return
        }

        val radius = radiusStr.toDoubleOrNull()
        if (radius == null || radius <= 0) {
            Toast.makeText(requireContext(), "Bitte gib einen gültigen Radius ein", Toast.LENGTH_SHORT).show()
            return
        }

        // Update current filter values
        currentZip = zip
        currentRadius = radius

        // Hide keyboard and collapse filter
        hideKeyboard()
        isFilterExpanded = false
        filterExpandedCard.visibility = View.GONE
        filterSummaryCard.visibility = View.VISIBLE
        updateFilterSummary()

        // Load events with new filter
        loadEvents(zip = zip, radiusKm = radius)
    }

    private fun loadEvents(zip: String, radiusKm: Double) {
        showLoading(true)
        val call = ApiClient.retrofit.getNearbyEvents(
            zip = zip,
            radius = radiusKm,
            sort = "start_date:asc",
            page = 1,
            pageSize = 25
        )
        call.enqueue(object : Callback<StrapiListResponse<EventItem>> {
            override fun onResponse(
                call: Call<StrapiListResponse<EventItem>>,
                response: Response<StrapiListResponse<EventItem>>
            ) {
                showLoading(false)
                if (response.isSuccessful) {
                    val items = (response.body()?.data ?: emptyList()).filter { ev ->
                        // Nur Events ab heute
                        val todayMidnight = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val start = parseIso(ev.attributes.startDate)
                        val end   = parseIso(ev.attributes.endDate)
                        (end != null && end >= todayMidnight) || (start != null && start >= todayMidnight)
                    }

                    if (items.isEmpty()) {
                        empty.text = "Keine Events gefunden."
                        empty.visibility = View.VISIBLE
                        adapter.submit(emptyList())
                    } else {
                        empty.visibility = View.GONE
                        adapter.submit(items)
                    }
                } else {
                    empty.text = "Laden fehlgeschlagen (${response.code()})"
                    empty.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<StrapiListResponse<EventItem>>, t: Throwable) {
                showLoading(false)
                empty.text = "Netzwerkfehler: ${t.message}"
                empty.visibility = View.VISIBLE
            }
        })
    }

    private fun parseIso(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)?.time
        }.getOrNull()
    }

    private fun onEventClicked(item: EventItem) {
        val eventId = item.attributes.documentId
            ?: item.id.toString()

        EventDetailActivity.start(requireContext(), eventId)
    }

    private fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        if (show) empty.visibility = View.GONE
    }
}
