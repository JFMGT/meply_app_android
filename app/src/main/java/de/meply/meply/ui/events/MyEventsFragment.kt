package de.meply.meply.ui.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.events.Event
import de.meply.meply.data.events.EventsResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyEventsFragment : Fragment() {

    companion object {
        private const val TAG = "MyEventsFragment"
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var eventsStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var eventsRecycler: RecyclerView
    private lateinit var btnCreateEmpty: Button

    private lateinit var adapter: MyEventsAdapter
    private val events = mutableListOf<Event>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupListeners()
        loadEvents()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        eventsStats = view.findViewById(R.id.events_stats)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        eventsRecycler = view.findViewById(R.id.events_recycler)
        btnCreateEmpty = view.findViewById(R.id.btn_create_empty)
    }

    private fun setupRecyclerView() {
        adapter = MyEventsAdapter(
            onEditClick = { event -> onEditEvent(event) },
            onDeleteClick = { event -> confirmDeleteEvent(event) }
        )

        eventsRecycler.layoutManager = LinearLayoutManager(requireContext())
        eventsRecycler.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            loadEvents()
        }

        btnCreateEmpty.setOnClickListener {
            openCreateEventSheet()
        }
    }

    fun loadEvents() {
        val profileDocId = AuthManager.getProfileDocumentId(requireContext())
        if (profileDocId == null) {
            eventsStats.text = "Nicht angemeldet"
            swipeRefresh.isRefreshing = false
            return
        }

        if (!swipeRefresh.isRefreshing) {
            loadingProgress.visibility = View.VISIBLE
        }
        eventsStats.text = "Lade Events..."

        ApiClient.retrofit.getMyEvents(profileDocId).enqueue(object : Callback<EventsResponse> {
            override fun onResponse(
                call: Call<EventsResponse>,
                response: Response<EventsResponse>
            ) {
                if (!isAdded) return

                swipeRefresh.isRefreshing = false
                loadingProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    events.clear()
                    events.addAll(data)
                    adapter.submitList(events.toList())
                    updateUI()
                } else {
                    eventsStats.text = "Fehler beim Laden (${response.code()})"
                    Log.e(TAG, "Error loading events: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<EventsResponse>, t: Throwable) {
                if (!isAdded) return

                swipeRefresh.isRefreshing = false
                loadingProgress.visibility = View.GONE
                eventsStats.text = "Netzwerkfehler"
                Log.e(TAG, "Network error loading events", t)
            }
        })
    }

    private fun updateUI() {
        val count = events.size
        eventsStats.text = when (count) {
            0 -> "Keine Events"
            1 -> "1 Event"
            else -> "$count Events"
        }

        if (count == 0) {
            emptyCard.visibility = View.VISIBLE
            eventsRecycler.visibility = View.GONE
        } else {
            emptyCard.visibility = View.GONE
            eventsRecycler.visibility = View.VISIBLE
        }
    }

    private fun onEditEvent(event: Event) {
        val sheet = EventBottomSheet.newInstanceWithEvent(event)
        sheet.setOnEventSavedListener {
            loadEvents()
        }
        sheet.show(childFragmentManager, "edit_event")
    }

    private fun confirmDeleteEvent(event: Event) {
        val documentId = event.documentId ?: return

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Event löschen?")
            .setMessage("Möchtest du \"${event.title}\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Löschen") { _, _ ->
                deleteEvent(documentId)
            }
            .show()
    }

    private fun deleteEvent(documentId: String) {
        loadingProgress.visibility = View.VISIBLE

        ApiClient.retrofit.deleteEvent(documentId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!isAdded) return

                loadingProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    events.removeAll { it.documentId == documentId }
                    adapter.submitList(events.toList())
                    updateUI()
                } else {
                    val message = when (response.code()) {
                        403 -> "Keine Berechtigung zum Löschen"
                        else -> "Fehler beim Löschen (${response.code()})"
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Fehler")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                    Log.e(TAG, "Error deleting event: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (!isAdded) return

                loadingProgress.visibility = View.GONE
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Fehler")
                    .setMessage("Netzwerkfehler: ${t.message}")
                    .setPositiveButton("OK", null)
                    .show()
                Log.e(TAG, "Network error deleting event", t)
            }
        })
    }

    fun openCreateEventSheet() {
        val sheet = EventBottomSheet.newInstance()
        sheet.setOnEventSavedListener {
            loadEvents()
        }
        sheet.show(childFragmentManager, "create_event")
    }
}
