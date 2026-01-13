package de.meply.meply.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.feed.ImageUploadResponse
import de.meply.meply.data.profile.InviteCodesResponse
import de.meply.meply.data.profile.ProfileItem
import de.meply.meply.data.profile.ProfileMeData
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.data.profile.UpdateProfileRequest
import de.meply.meply.network.ApiClient
import de.meply.meply.network.ApiService
import de.meply.meply.utils.AvatarUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var profileAvatar: ImageView
    private lateinit var btnChangeAvatar: Button
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
    private var currentAvatarUploadId: Int? = null
    private var photoUri: Uri? = null

    private val genderOptions = listOf("Keine Angabe", "Weiblich", "Männlich", "Divers", "anderes")

    // Activity Result Contracts
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            uploadAvatar(photoUri!!)
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAvatar(it) }
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Kamera-Berechtigung benötigt", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchGallery()
        } else {
            Toast.makeText(requireContext(), "Speicher-Berechtigung benötigt", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        progressBar = view.findViewById(R.id.profile_progress)
        profileAvatar = view.findViewById(R.id.profile_avatar)
        btnChangeAvatar = view.findViewById(R.id.btn_change_avatar)
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
        btnChangeAvatar.setOnClickListener { showAvatarOptions() }

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

        ApiClient.retrofit.getMyProfile()
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val profileResponse = response.body()
                        Log.d("ProfileFragment", "Profile response: $profileResponse")
                        val profileMeData = profileResponse?.data
                        if (profileMeData != null) {
                            Log.d("ProfileFragment", "Profile ID: ${profileMeData.id}")
                            Log.d("ProfileFragment", "Profile fields: username=${profileMeData.username}, city=${profileMeData.city}")
                            // Convert ProfileMeData to ProfileItem for use in the fragment
                            currentProfileId = profileMeData.id.toString()
                            loadedProfile = profileMeData.toProfileItem()
                            Log.d("ProfileFragment", "Converted ProfileItem attributes: ${loadedProfile?.attributes}")
                            showProfile(loadedProfile!!)
                        } else {
                            Log.e("ProfileFragment", "Profile data is null in response")
                            Toast.makeText(requireContext(), "Kein Profil gefunden", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("ProfileFragment", "Error loading profile: ${response.code()}")
                        Toast.makeText(requireContext(), "Fehler ${response.code()} beim Laden des Profils", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Network error loading profile", t)
                }
            })
    }

    private fun showProfile(profile: ProfileItem) {
        Log.d("ProfileFragment", "Loading profile: ${profile.attributes}")

        // Load avatar - For now we'll fetch from /profiles/me which returns avatar data
        loadProfileAvatar()

        editUsername.setText(profile.attributes?.username ?: "")
        editBirthDate.setText(profile.attributes?.birthDate ?: "")
        editPostal.setText(profile.attributes?.postalCode ?: "")
        editCity.setText(profile.attributes?.city ?: "")
        editRadius.setText(profile.attributes?.searchRadius?.toString() ?: "")
        editBggProfile.setText(profile.attributes?.boardgamegeekProfile ?: "")
        editBgaUsername.setText(profile.attributes?.boardGameArenaUsername ?: "")

        // Set gender spinner - convert from API format (English) to UI format (German)
        val genderApi = profile.attributes?.gender ?: "none"
        val genderDisplay = when (genderApi) {
            "none" -> "Keine Angabe"
            "female" -> "Weiblich"
            "male" -> "Männlich"
            "diverse" -> "Divers"
            "other" -> "anderes"
            else -> "Keine Angabe"
        }
        val genderIndex = genderOptions.indexOf(genderDisplay)
        if (genderIndex >= 0) {
            spinnerGender.setSelection(genderIndex)
        }

        // Set checkboxes
        // Use usersCanFollow if available, fallback to followPrivacy
        val privacySetting = profile.attributes?.usersCanFollow ?: profile.attributes?.followPrivacy
        checkboxFollowPrivacy.isChecked = privacySetting == "open"
        checkboxShowInUserList.isChecked = profile.attributes?.showInUserList ?: false
        checkboxAllowProfileView.isChecked = profile.attributes?.allowProfileView ?: false
        checkboxShowRatings.isChecked = profile.attributes?.showBoardGameRatings ?: false

        Log.d("ProfileFragment", "Privacy setting: $privacySetting, checked: ${checkboxFollowPrivacy.isChecked}")
    }

    private fun saveProfile() {
        val existing = loadedProfile
        if (existing == null) {
            Toast.makeText(requireContext(), "Kein Profil geladen", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Copy existing attributes
        val updateMap = existing.attributes?.toMutableMap()

        // Update with new values from form
        updateMap?.set("username", editUsername.text.toString())
        updateMap?.set("birthDate", editBirthDate.text.toString().ifEmpty { null })

        // Convert gender from German UI text to API format (English)
        val genderDisplay = spinnerGender.selectedItem.toString()
        val genderApi = when (genderDisplay) {
            "Keine Angabe" -> "none"
            "Weiblich" -> "female"
            "Männlich" -> "male"
            "Divers" -> "diverse"
            "anderes" -> "other"
            else -> "none"
        }
        updateMap?.set("gender", genderApi)

        updateMap?.set("postalCode", editPostal.text.toString())
        updateMap?.set("city", editCity.text.toString())
        updateMap?.set("searchRadius", editRadius.text.toString().toIntOrNull())
        updateMap?.set("boardgamegeekProfile", editBggProfile.text.toString().ifEmpty { null })
        updateMap?.set("boardGameArenaUsername", editBgaUsername.text.toString().ifEmpty { null })

        // usersCanFollow must be "open" or "request", not boolean
        val privacyValue = if (checkboxFollowPrivacy.isChecked) "open" else "request"
        updateMap?.set("usersCanFollow", privacyValue)
        // Also set followPrivacy for backwards compatibility
        updateMap?.set("followPrivacy", privacyValue)

        updateMap?.set("showInUserList", checkboxShowInUserList.isChecked)
        updateMap?.set("allowProfileView", checkboxAllowProfileView.isChecked)
        updateMap?.set("showBoardGameRatings", checkboxShowRatings.isChecked)

        val request = UpdateProfileRequest(updateMap)

        ApiClient.retrofit.updateMyProfile(request)
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Profil gespeichert", Toast.LENGTH_SHORT).show()
                        response.body()?.data?.let { profileMeData ->
                            // Convert ProfileMeData to ProfileItem
                            loadedProfile = profileMeData.toProfileItem()
                            showProfile(loadedProfile!!)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Fehler ${response.code()} beim Speichern", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Error saving profile: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
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

    private fun loadProfileAvatar() {
        // Fetch full profile with avatar from /profiles/me
        ApiClient.retrofit.getMyProfile()
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    if (response.isSuccessful) {
                        val profileData = response.body()?.data
                        // Check if avatar exists in the API response
                        // For now, use generated avatar based on user ID
                        val userId = profileData?.documentId ?: "default"
                        val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(userId)

                        Glide.with(this@ProfileFragment)
                            .load(defaultAvatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(profileAvatar)
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    Log.e("ProfileFragment", "Error loading avatar", t)
                }
            })
    }

    private fun showAvatarOptions() {
        val options = if (currentAvatarUploadId != null) {
            arrayOf("Foto aufnehmen", "Aus Galerie wählen", "Profilbild löschen", "Abbrechen")
        } else {
            arrayOf("Foto aufnehmen", "Aus Galerie wählen", "Abbrechen")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profilbild ändern")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Foto aufnehmen" -> checkCameraPermissionAndLaunch()
                    "Aus Galerie wählen" -> checkStoragePermissionAndLaunch()
                    "Profilbild löschen" -> confirmDeleteAvatar()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermissionAndLaunch() {
        when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+: Use READ_MEDIA_IMAGES
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        launchGallery()
                    }
                    else -> {
                        storagePermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            else -> {
                // Android 12 and below: Use READ_EXTERNAL_STORAGE
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        launchGallery()
                    }
                    else -> {
                        storagePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().externalCacheDir, "profile_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePicture.launch(photoUri)
    }

    private fun launchGallery() {
        pickImage.launch("image/*")
    }

    private fun uploadAvatar(uri: Uri) {
        showLoading(true)

        try {
            // Convert URI to File
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "avatar_upload_${System.currentTimeMillis()}.jpg")

            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            // Compress image if needed
            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: throw IllegalStateException("Could not open input stream for URI")

            val compressedFile = File(requireContext().cacheDir, "avatar_compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle() // Free memory

            // Create multipart request
            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("files", compressedFile.name, requestFile)

            // Upload to API
            ApiClient.retrofit.uploadImage(body)
                .enqueue(object : Callback<ImageUploadResponse> {
                    override fun onResponse(
                        call: Call<ImageUploadResponse>,
                        response: Response<ImageUploadResponse>
                    ) {
                        if (response.isSuccessful) {
                            val uploadId = response.body()?.uploads?.firstOrNull()?.uploadId
                            if (uploadId != null) {
                                currentAvatarUploadId = uploadId
                                updateProfileAvatar(uploadId)
                            } else {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Fehler beim Upload", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showLoading(false)
                            Toast.makeText(requireContext(), "Upload fehlgeschlagen: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }

                        // Clean up temp files
                        file.delete()
                        compressedFile.delete()
                    }

                    override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                        file.delete()
                    }
                })
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Fehler beim Verarbeiten des Bildes", Toast.LENGTH_SHORT).show()
            Log.e("ProfileFragment", "Error processing image", e)
        }
    }

    private fun updateProfileAvatar(uploadId: Int) {
        val updateMap = mutableMapOf<String, Any?>(
            "avatar" to listOf(uploadId)
        )

        val request = UpdateProfileRequest(updateMap)

        ApiClient.retrofit.updateMyProfile(request)
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Profilbild aktualisiert", Toast.LENGTH_SHORT).show()
                        loadProfileAvatar()
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmDeleteAvatar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profilbild löschen")
            .setMessage("Möchtest du dein Profilbild wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                deleteAvatar()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun deleteAvatar() {
        showLoading(true)

        val updateMap = mutableMapOf<String, Any?>(
            "avatar" to emptyList<Int>()
        )

        val request = UpdateProfileRequest(updateMap)

        ApiClient.retrofit.updateMyProfile(request)
            .enqueue(object : Callback<ProfileResponse<ProfileMeData>> {
                override fun onResponse(
                    call: Call<ProfileResponse<ProfileMeData>>,
                    response: Response<ProfileResponse<ProfileMeData>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        currentAvatarUploadId = null
                        Toast.makeText(requireContext(), "Profilbild gelöscht", Toast.LENGTH_SHORT).show()
                        loadProfileAvatar()
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            })
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
        usersCanFollow = null,  // Not available in ProfileData, will be fetched separately
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
        "usersCanFollow" to this.usersCanFollow,
        "followPrivacy" to this.followPrivacy,
        "allowProfileView" to this.allowProfileView,
        "showBoardGameRatings" to this.showBoardGameRatings,
        "latitude" to this.latitude,
        "longitude" to this.longitude,
        "cords" to this.cords
    )
}
