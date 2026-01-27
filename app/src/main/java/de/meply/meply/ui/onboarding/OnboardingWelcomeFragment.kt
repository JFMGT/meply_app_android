package de.meply.meply.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.network.ApiClient
import de.meply.meply.data.profile.ProfileMeData
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.data.profile.UpdateProfileRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OnboardingWelcomeFragment : Fragment(), OnboardingStepValidator {

    private lateinit var editPlz: TextInputEditText
    private var plzSaved = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editPlz = view.findViewById(R.id.edit_onboarding_plz)

        // Load existing PLZ if available
        loadExistingPlz()
    }

    private fun loadExistingPlz() {
        ApiClient.retrofit.getMyProfile()
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    if (response.isSuccessful) {
                        val plz = response.body()?.data?.postalCode
                        if (!plz.isNullOrBlank()) {
                            editPlz.setText(plz)
                            plzSaved = true
                        }
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    // Ignore
                }
            })
    }

    override fun canProceed(): Boolean {
        val plz = editPlz.text.toString().trim()
        if (plz.length < 4) {
            return false
        }

        // Save PLZ to profile
        savePlz(plz)
        return true
    }

    override fun showValidationError() {
        Toast.makeText(requireContext(), "Bitte gib deine Postleitzahl ein (min. 4 Zeichen)", Toast.LENGTH_SHORT).show()
    }

    private fun savePlz(plz: String) {
        val updateMap = mutableMapOf<String, Any?>(
            "postalCode" to plz
        )
        val request = UpdateProfileRequest(updateMap)

        ApiClient.retrofit.updateMyProfile(request)
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    if (response.isSuccessful) {
                        plzSaved = true
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    // Ignore, will be saved later
                }
            })
    }
}
