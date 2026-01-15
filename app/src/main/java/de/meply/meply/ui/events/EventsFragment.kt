package de.meply.meply.ui.events

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import de.meply.meply.R
import de.meply.meply.data.events.EventItem
import de.meply.meply.data.events.StrapiListResponse
import de.meply.meply.data.feed.LikeToggleRequest
import de.meply.meply.data.feed.LikeToggleResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventsFragment : Fragment() {

    private val DEFAULT_ZIP = "33334"
    private val DEFAULT_RADIUS_KM = 100000.0

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView
    private val adapter = EventsAdapter(
        onClick = { item -> onEventClicked(item) },
        onLikeClick = { item, position -> onEventLikeClicked(item, position) }
    )

    // Cache of loaded events (for updating liked state)
    private val loadedEvents = mutableListOf<EventItem>()

    private var currentZip: String? = null
    private var currentRadius: Double? = null
    private var hasUserSetFilter = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_events, container, false)
        recycler = v.findViewById(R.id.recyclerEvents)
        progress = v.findViewById(R.id.progress)
        empty = v.findViewById(R.id.emptyView)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Load user preferences from SharedPreferences
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedZip = prefs.getString("postalCode", null)
        val savedRadius = prefs.getInt("searchRadius", 0)

        // Load events with default or saved values
        val initialZip = if (!savedZip.isNullOrEmpty()) savedZip else DEFAULT_ZIP
        val initialRadius = if (savedRadius > 0) savedRadius.toDouble() else DEFAULT_RADIUS_KM

        // Check if user has previously set a filter
        hasUserSetFilter = !savedZip.isNullOrEmpty() && savedRadius > 0

        currentZip = initialZip
        currentRadius = initialRadius
        loadEvents(zip = initialZip, radiusKm = initialRadius)

        return v
    }

    fun showFilterBottomSheet() {
        val bottomSheet = EventFilterBottomSheet.newInstance(
            zip = if (hasUserSetFilter) currentZip else null,
            radius = if (hasUserSetFilter) currentRadius?.toInt() else null
        )

        bottomSheet.setOnFilterAppliedListener { zip, radius ->
            currentZip = zip
            currentRadius = radius
            hasUserSetFilter = true
            loadEvents(zip = zip, radiusKm = radius)
        }

        bottomSheet.setOnFilterResetListener {
            currentZip = DEFAULT_ZIP
            currentRadius = DEFAULT_RADIUS_KM
            hasUserSetFilter = false
            loadEvents(zip = DEFAULT_ZIP, radiusKm = DEFAULT_RADIUS_KM)
        }

        bottomSheet.show(parentFragmentManager, "eventFilter")
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
                        val end = parseIso(ev.attributes.endDate)
                        (end != null && end >= todayMidnight) || (start != null && start >= todayMidnight)
                    }

                    if (items.isEmpty()) {
                        empty.text = "Keine Events gefunden."
                        empty.visibility = View.VISIBLE
                        loadedEvents.clear()
                        adapter.submit(emptyList())
                    } else {
                        empty.visibility = View.GONE
                        loadedEvents.clear()
                        loadedEvents.addAll(items)
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

    private fun onEventLikeClicked(item: EventItem, position: Int) {
        val documentId = item.attributes.documentId ?: return

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
                    val newLikeCount = result?.getActualLikeCount() ?: 0
                    val isLiked = result?.status == "liked"

                    // Update the local cached event
                    if (position in loadedEvents.indices) {
                        val oldEvent = loadedEvents[position]
                        val updatedAttributes = oldEvent.attributes.copy(
                            likes = newLikeCount,
                            liked = isLiked
                        )
                        val updatedEvent = oldEvent.copy(attributes = updatedAttributes)
                        loadedEvents[position] = updatedEvent

                        // Refresh the list
                        adapter.submit(loadedEvents.toList())
                    }

                    Log.d("EventsFragment", "Like toggled: ${result?.status}, count: $newLikeCount")
                } else {
                    Log.e("EventsFragment", "Failed to toggle like: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LikeToggleResponse>, t: Throwable) {
                Log.e("EventsFragment", "Error toggling like", t)
            }
        })
    }

    private fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        if (show) empty.visibility = View.GONE
    }
}
