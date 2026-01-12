package de.meply.meply.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.profile.InviteCodesResponse
import de.meply.meply.data.profile.ProfileItem
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.data.profile.UpdateProfileRequest
import de.meply.meply.network.ApiClient
import de.meply.meply.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var editUsername: TextInputEditText
    private lateinit var editBirthDate: TextInputEditText
    private lateinit var spinnerGender: Spinner
    private lateinit var editPostal: TextInputEditText
    private lateinit var editCity: TextInputEditText
    private lateinit var editRadius: TextInputEditText
    private lateinit var editBggProfile: TextInputEditText
    private lateinit var editBgaUsername: TextInputEditText
    private lateinit var checkboxFollowPrivacy: CheckBox
    private lateinit var checkboxShowInUserList: CheckBox
    private lateinit var checkboxAllowProfileView: CheckBox
    private lateinit var checkboxShowRatings: CheckBox
    private lateinit var btnSave: Button
    private lateinit var inviteCodesInfo: TextView
    private lateinit var inviteCodesRecycler: RecyclerView

    private var loadedProfile: ProfileItem? = null
    private var currentProfileId: String? = null

    private val genderOptions = listOf("Keine Angabe", "Weiblich", "Männlich", "Divers", "anderes")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        progressBar = view.findViewById(R.id.profile_progress)
        editUsername = view.findViewById(R.id.edit_username)
        editBirthDate = view.findViewById(R.id.edit_birth_date)
        spinnerGender = view.findViewById(R.id.spinner_gender)
        editPostal = view.findViewById(R.id.edit_postal)
        editCity = view.findViewById(R.id.edit_city)
        editRadius = view.findViewById(R.id.edit_radius)
        editBggProfile = view.findViewById(R.id.edit_bgg_profile)
        editBgaUsername = view.findViewById(R.id.edit_bga_username)
        checkboxFollowPrivacy = view.findViewById(R.id.checkbox_follow_privacy)
        checkboxShowInUserList = view.findViewById(R.id.checkbox_show_in_user_list)
        checkboxAllowProfileView = view.findViewById(R.id.checkbox_allow_profile_view)
        checkboxShowRatings = view.findViewById(R.id.checkbox_show_ratings)
        btnSave = view.findViewById(R.id.btn_save)
        inviteCodesInfo = view.findViewById(R.id.invite_codes_info)
        inviteCodesRecycler = view.findViewById(R.id.invite_codes_recycler)

        setupGenderSpinner()
        setupInviteCodesRecycler()

        btnSave.setOnClickListener { saveProfile() }

        loadProfile()
        loadInviteCodes()

        return view
    }

    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            genderOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter
    }

    private fun setupInviteCodesRecycler() {
        inviteCodesRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadProfile() {
        showLoading(true)

        ApiClient.retrofit.getCurrentUser()
            .enqueue(object : Callback<ApiService.UserMe> {
                override fun onResponse(
                    call: Call<ApiService.UserMe>,
                    response: Response<ApiService.UserMe>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val user = response.body()
                        val profile = user?.profile
                        if (profile != null) {
                            currentProfileId = profile.documentId
                            loadedProfile = ProfileItem(profile.id, profile.attributes)
                            showProfile(loadedProfile!!)
                        } else {
                            Toast.makeText(requireContext(), "Kein Profil gefunden", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Fehler ${response.code()} beim Laden des Users", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiService.UserMe>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showProfile(profile: ProfileItem) {
        editUsername.setText(profile.attributes?.username ?: "")
        editBirthDate.setText(profile.attributes?.birthDate ?: "")
        editPostal.setText(profile.attributes?.postalCode ?: "")
        editCity.setText(profile.attributes?.city ?: "")
        editRadius.setText(profile.attributes?.searchRadius?.toString() ?: "")
        editBggProfile.setText(profile.attributes?.boardgamegeekProfile ?: "")
        editBgaUsername.setText(profile.attributes?.boardGameArenaUsername ?: "")

        // Set gender spinner
        val gender = profile.attributes?.gender ?: "Keine Angabe"
        val genderIndex = genderOptions.indexOf(gender)
        if (genderIndex >= 0) {
            spinnerGender.setSelection(genderIndex)
        }

        // Set checkboxes
        // followPrivacy is "open" or "request", checkbox means "open" (users can follow without confirmation)
        checkboxFollowPrivacy.isChecked = profile.attributes?.followPrivacy == "open"
        checkboxShowInUserList.isChecked = profile.attributes?.showInUserList ?: false
        checkboxAllowProfileView.isChecked = profile.attributes?.allowProfileView ?: false
        checkboxShowRatings.isChecked = profile.attributes?.showBoardGameRatings ?: false
    }

    private fun saveProfile() {
        val profileId = currentProfileId
        val existing = loadedProfile
        if (profileId.isNullOrBlank() || existing == null) {
            Toast.makeText(requireContext(), "Kein Profil geladen", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Copy existing attributes
        val updateMap = existing.attributes?.toMutableMap()

        // Update with new values from form
        updateMap?.set("username", editUsername.text.toString())
        updateMap?.set("birthDate", editBirthDate.text.toString().ifEmpty { null })
        updateMap?.set("gender", spinnerGender.selectedItem.toString())
        updateMap?.set("postalCode", editPostal.text.toString())
        updateMap?.set("city", editCity.text.toString())
        updateMap?.set("searchRadius", editRadius.text.toString().toIntOrNull())
        updateMap?.set("boardgamegeekProfile", editBggProfile.text.toString().ifEmpty { null })
        updateMap?.set("boardGameArenaUsername", editBgaUsername.text.toString().ifEmpty { null })
        // followPrivacy must be "open" or "request", not boolean
        updateMap?.set("followPrivacy", if (checkboxFollowPrivacy.isChecked) "open" else "request")
        updateMap?.set("showInUserList", checkboxShowInUserList.isChecked)
        updateMap?.set("allowProfileView", checkboxAllowProfileView.isChecked)
        updateMap?.set("showBoardGameRatings", checkboxShowRatings.isChecked)

        val request = UpdateProfileRequest(updateMap)

        ApiClient.retrofit.updateProfile(profileId, request)
            .enqueue(object : Callback<ProfileResponse<ProfileItem>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileItem>>,
                    response: Response<ProfileResponse<ProfileItem>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Profil gespeichert", Toast.LENGTH_SHORT).show()
                        response.body()?.data?.let {
                            loadedProfile = it
                            showProfile(it)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Fehler ${response.code()} beim Speichern", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Error saving profile: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileItem>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Network error saving profile", t)
                }
            })
    }

    private fun loadInviteCodes() {
        ApiClient.retrofit.getMyInviteCodes()
            .enqueue(object : Callback<InviteCodesResponse> {
                override fun onResponse(
                    call: Call<InviteCodesResponse>,
                    response: Response<InviteCodesResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        if (data != null) {
                            showInviteCodes(data.codes, data.stats.totalCodes, data.stats.unusedCodes)
                        }
                    } else {
                        Log.e("ProfileFragment", "Error loading invite codes: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<InviteCodesResponse>, t: Throwable) {
                    Log.e("ProfileFragment", "Network error loading invite codes", t)
                }
            })
    }

    private fun showInviteCodes(codes: List<de.meply.meply.data.profile.InviteCode>, total: Int, unused: Int) {
        inviteCodesInfo.text = "Du hast $total Codes, davon $unused unbenutzt. Teile einen Code mit einer Person, die du einladen möchtest."

        val adapter = InviteCodeAdapter(codes)
        inviteCodesRecycler.adapter = adapter
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}

/**
 * Helper functions for mapping
 */
fun ApiService.ProfileData.toAttributes(): de.meply.meply.data.profile.ProfileAttributes {
    return de.meply.meply.data.profile.ProfileAttributes(
        username = this.username,
        birthDate = this.birthDate,
        postalCode = this.postalCode,
        city = this.city,
        searchRadius = this.searchRadius,
        gender = this.gender,
        boardgamegeekProfile = this.boardgamegeekProfile,
        boardGameArenaUsername = this.boardGameArenaUsername,
        showInUserList = this.showInUserList,
        followPrivacy = this.followPrivacy,
        allowProfileView = this.allowProfileView,
        showBoardGameRatings = this.showBoardGameRatings,
        latitude = this.latitude,
        longitude = this.longitude,
        cords = this.cords
    )
}

fun de.meply.meply.data.profile.ProfileAttributes.toMutableMap(): MutableMap<String, Any?> {
    return mutableMapOf(
        "username" to this.username,
        "birthDate" to this.birthDate,
        "postalCode" to this.postalCode,
        "city" to this.city,
        "searchRadius" to this.searchRadius,
        "gender" to this.gender,
        "boardgamegeekProfile" to this.boardgamegeekProfile,
        "boardGameArenaUsername" to this.boardGameArenaUsername,
        "showInUserList" to this.showInUserList,
        "followPrivacy" to this.followPrivacy,
        "allowProfileView" to this.allowProfileView,
        "showBoardGameRatings" to this.showBoardGameRatings,
        "latitude" to this.latitude,
        "longitude" to this.longitude,
        "cords" to this.cords
    )
}
