package de.meply.meply.ui.locations

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import de.meply.meply.R
import de.meply.meply.data.locations.Location
import de.meply.meply.data.locations.LocationsResponse
import de.meply.meply.data.profile.ProfileMeData
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationsOverviewFragment : Fragment() {

    companion object {
        private const val TAG = "LocationsOverview"
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var locationsStats: TextView
    private lateinit var filterButton: View
    private lateinit var activeFilterText: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var emptyHint: TextView
    private lateinit var locationsRecycler: RecyclerView

    private lateinit var adapter: LocationsOverviewAdapter
    private val locations = mutableListOf<LocationsOverviewAdapter.LocationWithDistance>()

    // Filter state
    private var currentPlz: String? = null
    private var currentRadius: Int = 50
    private var currentType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_locations_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupListeners()
        loadInitialPLZ()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        locationsStats = view.findViewById(R.id.locations_stats)
        filterButton = view.findViewById(R.id.filterButton)
        activeFilterText = view.findViewById(R.id.activeFilterText)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        emptyHint = view.findViewById(R.id.empty_hint)
        locationsRecycler = view.findViewById(R.id.locations_recycler)
    }

    private fun setupRecyclerView() {
        adapter = LocationsOverviewAdapter { location ->
            onLocationClick(location)
        }

        locationsRecycler.layoutManager = LinearLayoutManager(requireContext())
        locationsRecycler.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            loadLocations()
        }

        filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = LocationsFilterBottomSheet.newInstance(
            plz = currentPlz,
            radius = currentRadius,
            type = currentType
        )

        bottomSheet.setOnFilterAppliedListener { filterValues ->
            currentPlz = filterValues.plz
            currentRadius = filterValues.radius
            currentType = filterValues.type
            updateActiveFilterIndicator()
            loadLocations()
        }

        bottomSheet.setOnFilterResetListener {
            currentPlz = null
            currentRadius = 50
            currentType = null
            updateActiveFilterIndicator()
            loadLocations()
        }

        bottomSheet.show(childFragmentManager, "locationsFilter")
    }

    private fun updateActiveFilterIndicator() {
        val filters = mutableListOf<String>()

        currentPlz?.takeIf { it.length == 5 }?.let {
            filters.add("PLZ: $it")
            filters.add("$currentRadius km")
        }

        currentType?.let {
            filters.add(it)
        }

        if (filters.isNotEmpty()) {
            activeFilterText.text = filters.joinToString(" • ")
            activeFilterText.visibility = View.VISIBLE
        } else {
            activeFilterText.visibility = View.GONE
        }
    }

    private fun loadInitialPLZ() {
        // Try to get user's PLZ from profile API
        ApiClient.retrofit.getMyProfile().enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
            override fun onResponse(
                call: Call<ProfileResponse<ProfileMeData>>,
                response: Response<ProfileResponse<ProfileMeData>>
            ) {
                if (!isAdded) return
                if (response.isSuccessful) {
                    val postalCode = response.body()?.data?.postalCode
                    if (!postalCode.isNullOrBlank()) {
                        currentPlz = postalCode
                        updateActiveFilterIndicator()
                    }
                }
                // Load locations after attempting to get PLZ
                loadLocations()
            }

            override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                // Silently fail - PLZ prefill is not critical
                Log.d(TAG, "Could not load initial PLZ: ${t.message}")
                loadLocations()
            }
        })
    }

    fun loadLocations() {
        if (!swipeRefresh.isRefreshing) {
            loadingProgress.visibility = View.VISIBLE
        }
        locationsStats.text = "Suche Locations..."

        // Decide which API to call based on whether PLZ is provided
        if (!currentPlz.isNullOrBlank() && currentPlz!!.length == 5) {
            // Use nearby endpoint with PLZ and radius
            loadNearbyLocations(currentPlz!!)
        } else {
            // Use public endpoint (all locations)
            loadAllLocations()
        }
    }

    private fun loadNearbyLocations(plz: String) {
        ApiClient.retrofit.getNearbyLocations(
            zip = plz,
            radius = currentRadius,
            type = currentType
        ).enqueue(object : Callback<LocationsResponse> {
            override fun onResponse(
                call: Call<LocationsResponse>,
                response: Response<LocationsResponse>
            ) {
                handleResponse(response, true)
            }

            override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                handleError(t)
            }
        })
    }

    private fun loadAllLocations() {
        ApiClient.retrofit.getPublicLocations(
            type = currentType
        ).enqueue(object : Callback<LocationsResponse> {
            override fun onResponse(
                call: Call<LocationsResponse>,
                response: Response<LocationsResponse>
            ) {
                handleResponse(response, false)
            }

            override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                handleError(t)
            }
        })
    }

    private fun handleResponse(response: Response<LocationsResponse>, hasDistance: Boolean) {
        if (!isAdded) return

        swipeRefresh.isRefreshing = false
        loadingProgress.visibility = View.GONE

        if (response.isSuccessful) {
            val data = response.body()?.data ?: emptyList()
            locations.clear()

            // Convert to LocationWithDistance
            locations.addAll(data.map { LocationsOverviewAdapter.LocationWithDistance(it, null) })

            adapter.submitList(locations.toList())
            updateUI()
        } else {
            val errorCode = response.code()
            if (errorCode == 404) {
                // Nearby endpoint might not exist, fallback to public
                locationsStats.text = "Umkreissuche nicht verfügbar"
                loadAllLocations()
            } else {
                locationsStats.text = "Fehler beim Laden (${errorCode})"
                Log.e(TAG, "Error loading locations: ${response.errorBody()?.string()}")
            }
        }
    }

    private fun handleError(t: Throwable) {
        if (!isAdded) return

        swipeRefresh.isRefreshing = false
        loadingProgress.visibility = View.GONE
        locationsStats.text = "Netzwerkfehler"
        Log.e(TAG, "Network error loading locations", t)
    }

    private fun updateUI() {
        val count = locations.size

        locationsStats.text = when {
            count == 0 -> "Keine Locations gefunden"
            !currentPlz.isNullOrBlank() && currentPlz!!.length == 5 -> "$count Locations im Umkreis von $currentRadius km"
            count == 1 -> "1 Location"
            else -> "$count Locations"
        }

        if (count == 0) {
            emptyCard.visibility = View.VISIBLE
            locationsRecycler.visibility = View.GONE

            // Update hint based on search type
            emptyHint.text = if (!currentPlz.isNullOrBlank()) {
                "Versuche einen größeren Suchradius oder eine andere PLZ."
            } else {
                "Tippe auf 'Filter' um nach PLZ und Umkreis zu suchen."
            }
        } else {
            emptyCard.visibility = View.GONE
            locationsRecycler.visibility = View.VISIBLE
        }
    }

    private fun onLocationClick(location: Location) {
        // Open in browser if website available
        if (!location.website.isNullOrBlank()) {
            val url = if (location.website.startsWith("http")) location.website else "https://${location.website}"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        }
    }
}
