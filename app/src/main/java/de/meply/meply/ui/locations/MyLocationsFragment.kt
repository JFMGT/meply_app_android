package de.meply.meply.ui.locations

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
import de.meply.meply.data.locations.Location
import de.meply.meply.data.locations.LocationsResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyLocationsFragment : Fragment() {

    companion object {
        private const val TAG = "MyLocationsFragment"
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var locationsStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var locationsRecycler: RecyclerView
    private lateinit var btnCreateEmpty: Button

    private lateinit var adapter: LocationsAdapter
    private val locations = mutableListOf<Location>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_locations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupListeners()
        loadLocations()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        locationsStats = view.findViewById(R.id.locations_stats)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        locationsRecycler = view.findViewById(R.id.locations_recycler)
        btnCreateEmpty = view.findViewById(R.id.btn_create_empty)
    }

    private fun setupRecyclerView() {
        adapter = LocationsAdapter(
            onEditClick = { location -> onEditLocation(location) },
            onDeleteClick = { location -> confirmDeleteLocation(location) }
        )

        locationsRecycler.layoutManager = LinearLayoutManager(requireContext())
        locationsRecycler.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            loadLocations()
        }

        btnCreateEmpty.setOnClickListener {
            openCreateLocationSheet()
        }
    }

    fun loadLocations() {
        val profileDocId = AuthManager.getProfileDocumentId(requireContext())
        if (profileDocId == null) {
            locationsStats.text = "Nicht angemeldet"
            swipeRefresh.isRefreshing = false
            return
        }

        if (!swipeRefresh.isRefreshing) {
            loadingProgress.visibility = View.VISIBLE
        }
        locationsStats.text = "Lade Locations..."

        ApiClient.retrofit.getMyLocations(profileDocId).enqueue(object : Callback<LocationsResponse> {
            override fun onResponse(
                call: Call<LocationsResponse>,
                response: Response<LocationsResponse>
            ) {
                if (!isAdded) return

                swipeRefresh.isRefreshing = false
                loadingProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    locations.clear()
                    locations.addAll(data)
                    adapter.submitList(locations.toList())
                    updateUI()
                } else {
                    locationsStats.text = "Fehler beim Laden (${response.code()})"
                    Log.e(TAG, "Error loading locations: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                if (!isAdded) return

                swipeRefresh.isRefreshing = false
                loadingProgress.visibility = View.GONE
                locationsStats.text = "Netzwerkfehler"
                Log.e(TAG, "Network error loading locations", t)
            }
        })
    }

    private fun updateUI() {
        val count = locations.size
        locationsStats.text = when (count) {
            0 -> "Keine Locations"
            1 -> "1 Location"
            else -> "$count Locations"
        }

        if (count == 0) {
            emptyCard.visibility = View.VISIBLE
            locationsRecycler.visibility = View.GONE
        } else {
            emptyCard.visibility = View.GONE
            locationsRecycler.visibility = View.VISIBLE
        }
    }

    private fun onEditLocation(location: Location) {
        val sheet = LocationBottomSheet.newInstanceWithLocation(location)
        sheet.setOnLocationSavedListener {
            loadLocations()
        }
        sheet.show(childFragmentManager, "edit_location")
    }

    private fun confirmDeleteLocation(location: Location) {
        val documentId = location.documentId ?: return

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Location löschen?")
            .setMessage("Möchtest du \"${location.titel}\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Löschen") { _, _ ->
                deleteLocation(documentId)
            }
            .show()
    }

    private fun deleteLocation(documentId: String) {
        loadingProgress.visibility = View.VISIBLE

        ApiClient.retrofit.deleteLocation(documentId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!isAdded) return

                loadingProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    // Remove from local list and refresh
                    locations.removeAll { it.documentId == documentId }
                    adapter.submitList(locations.toList())
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
                    Log.e(TAG, "Error deleting location: ${response.errorBody()?.string()}")
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
                Log.e(TAG, "Network error deleting location", t)
            }
        })
    }

    fun openCreateLocationSheet() {
        val sheet = LocationBottomSheet.newInstance()
        sheet.setOnLocationSavedListener {
            loadLocations()
        }
        sheet.show(childFragmentManager, "create_location")
    }
}
