package de.meply.meply.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import de.meply.meply.R
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
    private lateinit var editUsername: EditText
    private lateinit var editCity: EditText
    private lateinit var editPostal: EditText
    private lateinit var editRadius: EditText
    private lateinit var btnSave: Button

    private var loadedProfile: ProfileItem? = null
    private var currentProfileId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        progressBar = view.findViewById(R.id.profile_progress)
        editUsername = view.findViewById(R.id.edit_username)
        editCity = view.findViewById(R.id.edit_city)
        editPostal = view.findViewById(R.id.edit_postal)
        editRadius = view.findViewById(R.id.edit_radius)
        btnSave = view.findViewById(R.id.btn_save)

        btnSave.setOnClickListener { saveProfile() }

        loadProfile()

        return view
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
        editCity.setText(profile.attributes?.city ?: "")
        editPostal.setText(profile.attributes?.postalCode ?: "")
        editRadius.setText(profile.attributes?.searchRadius?.toString() ?: "")
    }

    private fun saveProfile() {
        val profileId = currentProfileId
        val existing = loadedProfile
        if (profileId.isNullOrBlank() || existing == null) {
            Toast.makeText(requireContext(), "Kein Profil geladen", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // vorhandene Attribute kopieren
        val updateMap = existing.attributes?.toMutableMap()

        // überschreiben mit neuen Werten aus Formular
        updateMap?.set("username", editUsername.text.toString())
        updateMap?.set("city", editCity.text.toString())
        updateMap?.set("postalCode", editPostal.text.toString())
        updateMap?.set("searchRadius", editRadius.text.toString().toIntOrNull())

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
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileItem>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}

/**
 * Hilfsfunktionen fürs Mapping
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
