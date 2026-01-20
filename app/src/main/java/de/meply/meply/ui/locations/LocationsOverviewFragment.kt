package de.meply.meply.ui.locations

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
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
        private val RADIUS_OPTIONS = listOf(
            "10 km" to 10,
            "25 km" to 25,
            "50 km" to 50,
            "100 km" to 100,
            "200 km" to 200
        )
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var locationsStats: TextView
    private lateinit var plzInput: TextInputEditText
    private lateinit var radiusSpinner: AutoCompleteTextView
    private lateinit var typeChipGroup: ChipGroup
    private lateinit var btnSearch: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var emptyHint: TextView
    private lateinit var locationsRecycler: RecyclerView

    private lateinit var adapter: LocationsOverviewAdapter
    private val locations = mutableListOf<LocationsOverviewAdapter.LocationWithDistance>()

    private var selectedType: String? = null
    private var selectedRadius: Int = 50

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
        setupRadiusSpinner()
        setupTypeChips()
        setupRecyclerView()
        setupListeners()
        loadInitialPLZ()
        loadLocations()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        locationsStats = view.findViewById(R.id.locations_stats)
        plzInput = view.findViewById(R.id.plzInput)
        radiusSpinner = view.findViewById(R.id.radiusSpinner)
        typeChipGroup = view.findViewById(R.id.typeChipGroup)
        btnSearch = view.findViewById(R.id.btnSearch)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        emptyHint = view.findViewById(R.id.empty_hint)
        locationsRecycler = view.findViewById(R.id.locations_recycler)
    }

    private fun setupRadiusSpinner() {
        val radiusLabels = RADIUS_OPTIONS.map { it.first }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, radiusLabels)
        radiusSpinner.setAdapter(adapter)
        radiusSpinner.setText("50 km", false)

        radiusSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedRadius = RADIUS_OPTIONS[position].second
        }
    }

    private fun setupTypeChips() {
        typeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = when {
                checkedIds.contains(R.id.chipGeschaeft) -> "Geschäft"
                checkedIds.contains(R.id.chipCafe) -> "Cafe"
                checkedIds.contains(R.id.chipClub) -> "Club"
                checkedIds.contains(R.id.chipLocation) -> "Location"
                else -> null
            }
        }
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

        btnSearch.setOnClickListener {
            hideKeyboard()
            loadLocations()
        }

        plzInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                loadLocations()
                true
            } else {
                false
            }
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
                        plzInput.setText(postalCode)
                    }
                }
            }

            override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                // Silently fail - PLZ prefill is not critical
                Log.d(TAG, "Could not load initial PLZ: ${t.message}")
            }
        })
    }

    fun loadLocations() {
        val plz = plzInput.text?.toString()?.trim()

        if (!swipeRefresh.isRefreshing) {
            loadingProgress.visibility = View.VISIBLE
        }
        locationsStats.text = "Suche Locations..."

        // Decide which API to call based on whether PLZ is provided
        if (!plz.isNullOrBlank() && plz.length == 5) {
            // Use nearby endpoint with PLZ and radius
            loadNearbyLocations(plz)
        } else {
            // Use public endpoint (all locations)
            loadAllLocations()
        }
    }

    private fun loadNearbyLocations(plz: String) {
        ApiClient.retrofit.getNearbyLocations(
            zip = plz,
            radius = selectedRadius,
            type = selectedType
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
            type = selectedType
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
            // Note: The nearby endpoint might return distance in the response
            // For now, we'll set distance to null as the API structure isn't clear
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
        val plz = plzInput.text?.toString()?.trim()

        locationsStats.text = when {
            count == 0 -> "Keine Locations gefunden"
            !plz.isNullOrBlank() && plz.length == 5 -> "$count Locations im Umkreis von $selectedRadius km"
            count == 1 -> "1 Location"
            else -> "$count Locations"
        }

        if (count == 0) {
            emptyCard.visibility = View.VISIBLE
            locationsRecycler.visibility = View.GONE

            // Update hint based on search type
            emptyHint.text = if (!plzInput.text.isNullOrBlank()) {
                "Versuche einen größeren Suchradius oder eine andere PLZ."
            } else {
                "Gib eine PLZ ein um Locations in deiner Nähe zu finden."
            }
        } else {
            emptyCard.visibility = View.GONE
            locationsRecycler.visibility = View.VISIBLE
        }
    }

    private fun onLocationClick(location: Location) {
        // TODO: Open location detail page or WebView
        // For now, open in browser if website available
        if (!location.website.isNullOrBlank()) {
            val url = if (location.website.startsWith("http")) location.website else "https://${location.website}"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(plzInput.windowToken, 0)
    }
}
