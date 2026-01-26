package de.meply.meply.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.exifinterface.media.ExifInterface
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
import de.meply.meply.LoginActivity
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.feed.ImageUploadResponse
import de.meply.meply.data.feed.StrapiUploadResponse
import de.meply.meply.data.availability.AvailabilityActionResponse
import de.meply.meply.data.availability.SetAvailabilityRequest
import de.meply.meply.data.availability.UserAvailability
import de.meply.meply.data.availability.UserAvailabilityResponse
import de.meply.meply.data.profile.InviteCodesResponse
import de.meply.meply.data.profile.ProfileItem
import de.meply.meply.data.profile.ProfileMeData
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.data.profile.UpdateProfileRequest
import de.meply.meply.network.ApiClient
import de.meply.meply.network.ApiService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import de.meply.meply.utils.AvatarUtils
import android.content.Intent
import com.google.android.material.card.MaterialCardView
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
    private lateinit var checkboxPushEnabled: CheckBox
    private lateinit var spinnerPushFrequency: Spinner
    private lateinit var editPushQuietStart: TextInputEditText
    private lateinit var editPushQuietEnd: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var inviteCodesInfo: TextView
    private lateinit var inviteCodesRecycler: RecyclerView
    private lateinit var deletionWarningCard: MaterialCardView
    private lateinit var deletionWarningText: TextView
    private lateinit var btnCancelDeletion: Button
    private lateinit var btnDeleteAccount: Button

    // Availability (Spielbereit) Views
    private lateinit var availabilityCard: MaterialCardView
    private lateinit var availabilityProgress: ProgressBar
    private lateinit var availabilityInactive: View
    private lateinit var availabilityActive: View
    private lateinit var btnSetAvailable: Button
    private lateinit var btnEditAvailability: Button
    private lateinit var btnRemoveAvailability: Button
    private lateinit var availabilityStatusText: TextView
    private lateinit var availabilityExpiresText: TextView
    private lateinit var availabilityHostingText: TextView
    private lateinit var availabilityNoteText: TextView

    private var currentAvailability: UserAvailability? = null
    private var loadedProfile: ProfileItem? = null
    private var currentScheduledDeletionAt: String? = null
    private var currentProfileId: String? = null
    private var currentAvatarUploadId: Int? = null
    private var photoUri: Uri? = null

    private val genderOptions = listOf("Keine Angabe", "Weiblich", "Männlich", "Divers", "anderes")
    private val pushFrequencyOptions = listOf("Sofort", "Stündlich", "Alle 4 Stunden", "Täglich")
    private val pushFrequencyValues = listOf("instant", "hourly", "every4hours", "daily")

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
        checkboxPushEnabled = view.findViewById(R.id.checkbox_push_enabled)
        spinnerPushFrequency = view.findViewById(R.id.spinner_push_frequency)
        editPushQuietStart = view.findViewById(R.id.edit_push_quiet_start)
        editPushQuietEnd = view.findViewById(R.id.edit_push_quiet_end)
        btnSave = view.findViewById(R.id.btn_save)
        inviteCodesInfo = view.findViewById(R.id.invite_codes_info)
        inviteCodesRecycler = view.findViewById(R.id.invite_codes_recycler)
        deletionWarningCard = view.findViewById(R.id.deletion_warning_card)
        deletionWarningText = view.findViewById(R.id.deletion_warning_text)
        btnCancelDeletion = view.findViewById(R.id.btn_cancel_deletion)
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account)

        // Initialize Availability Views
        availabilityCard = view.findViewById(R.id.availability_card)
        availabilityProgress = view.findViewById(R.id.availability_progress)
        availabilityInactive = view.findViewById(R.id.availability_inactive)
        availabilityActive = view.findViewById(R.id.availability_active)
        btnSetAvailable = view.findViewById(R.id.btn_set_available)
        btnEditAvailability = view.findViewById(R.id.btn_edit_availability)
        btnRemoveAvailability = view.findViewById(R.id.btn_remove_availability)
        availabilityStatusText = view.findViewById(R.id.availability_status_text)
        availabilityExpiresText = view.findViewById(R.id.availability_expires_text)
        availabilityHostingText = view.findViewById(R.id.availability_hosting_text)
        availabilityNoteText = view.findViewById(R.id.availability_note_text)

        setupGenderSpinner()
        setupPushFrequencySpinner()
        setupInviteCodesRecycler()
        setupTimePickerFields()

        btnSave.setOnClickListener { saveProfile() }
        btnChangeAvatar.setOnClickListener { showAvatarOptions() }
        btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }
        btnCancelDeletion.setOnClickListener { cancelAccountDeletion() }

        // Availability button listeners
        btnSetAvailable.setOnClickListener { showAvailabilityBottomSheet() }
        btnEditAvailability.setOnClickListener { showAvailabilityBottomSheet(currentAvailability) }
        btnRemoveAvailability.setOnClickListener { confirmRemoveAvailability() }

        loadProfile()
        loadInviteCodes()
        loadMyAvailability()

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

    private fun setupPushFrequencySpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            pushFrequencyOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPushFrequency.adapter = adapter
    }

    private fun setupInviteCodesRecycler() {
        inviteCodesRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupTimePickerFields() {
        // Make time fields non-editable (user must use TimePicker)
        editPushQuietStart.isFocusable = false
        editPushQuietStart.isClickable = true
        editPushQuietEnd.isFocusable = false
        editPushQuietEnd.isClickable = true

        editPushQuietStart.setOnClickListener { showTimePicker(editPushQuietStart) }
        editPushQuietEnd.setOnClickListener { showTimePicker(editPushQuietEnd) }
    }

    private fun showTimePicker(targetField: TextInputEditText) {
        // Parse existing time or use defaults
        val currentText = targetField.text.toString()
        val (hour, minute) = if (currentText.isNotEmpty() && currentText.contains(":")) {
            val parts = currentText.split(":")
            Pair(parts[0].toIntOrNull() ?: 22, parts[1].toIntOrNull() ?: 0)
        } else {
            // Default times: 22:00 for start, 08:00 for end
            if (targetField == editPushQuietStart) Pair(22, 0) else Pair(8, 0)
        }

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                // Format as HH:mm (with leading zeros)
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                targetField.setText(formattedTime)
            },
            hour,
            minute,
            true // 24-hour format
        ).show()
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

                            // Handle scheduled deletion
                            currentScheduledDeletionAt = profileMeData.scheduledDeletionAt
                            AuthManager.saveScheduledDeletionAt(requireContext(), currentScheduledDeletionAt)
                            updateDeletionUI()

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

        // Set push notification settings
        checkboxPushEnabled.isChecked = profile.attributes?.pushEnabled ?: true

        // Set push frequency spinner
        val pushFrequencyApi = profile.attributes?.pushFrequency ?: "instant"
        val pushFrequencyIndex = pushFrequencyValues.indexOf(pushFrequencyApi)
        if (pushFrequencyIndex >= 0) {
            spinnerPushFrequency.setSelection(pushFrequencyIndex)
        }

        // Set quiet time fields
        editPushQuietStart.setText(profile.attributes?.pushQuietStart ?: "")
        editPushQuietEnd.setText(profile.attributes?.pushQuietEnd ?: "")

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

        // Push notification settings
        updateMap?.set("pushEnabled", checkboxPushEnabled.isChecked)
        val pushFrequencyIndex = spinnerPushFrequency.selectedItemPosition
        updateMap?.set("pushFrequency", pushFrequencyValues.getOrElse(pushFrequencyIndex) { "instant" })
        updateMap?.set("pushQuietStart", editPushQuietStart.text.toString().ifEmpty { null })
        updateMap?.set("pushQuietEnd", editPushQuietEnd.text.toString().ifEmpty { null })

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
                        val userId = profileData?.documentId ?: "default"

                        // Check if user has uploaded avatar
                        val avatarUrl = profileData?.avatar?.firstOrNull()?.url

                        if (!avatarUrl.isNullOrEmpty()) {
                            // User has uploaded avatar - load it
                            val fullUrl = "${ApiClient.STRAPI_IMAGE_BASE}$avatarUrl"
                            currentAvatarUploadId = profileData?.avatar?.firstOrNull()?.id

                            Glide.with(this@ProfileFragment)
                                .load(fullUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(profileAvatar)
                        } else {
                            // No uploaded avatar - use generated one
                            val defaultAvatarUrl = AvatarUtils.getDefaultAvatarUrl(userId)
                            currentAvatarUploadId = null

                            Glide.with(this@ProfileFragment)
                                .load(defaultAvatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(profileAvatar)
                        }
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

        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
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

    /**
     * Fix image orientation based on EXIF data
     * Camera photos often have incorrect orientation that needs to be corrected
     */
    private fun fixImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            return if (rotationDegrees != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees)
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                // Recycle old bitmap if it's different from the new one
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error fixing image orientation", e)
            return bitmap
        }
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

            // Load and rotate bitmap based on EXIF orientation
            var bitmap = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: throw IllegalStateException("Could not open input stream for URI")

            // Fix orientation based on EXIF data
            bitmap = fixImageOrientation(uri, bitmap)

            val compressedFile = File(requireContext().cacheDir, "avatar_compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle() // Free memory

            // Create multipart request
            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("files", compressedFile.name, requestFile)

            // Upload to API using standard Strapi upload endpoint
            ApiClient.retrofit.uploadFile(body)
                .enqueue(object : Callback<List<StrapiUploadResponse>> {
                    override fun onResponse(
                        call: Call<List<StrapiUploadResponse>>,
                        response: Response<List<StrapiUploadResponse>>
                    ) {
                        if (response.isSuccessful) {
                            val uploadId = response.body()?.firstOrNull()?.id
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

                    override fun onFailure(call: Call<List<StrapiUploadResponse>>, t: Throwable) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                        file.delete()
                        compressedFile.delete()
                    }
                })
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(requireContext(), "Fehler beim Verarbeiten des Bildes", Toast.LENGTH_SHORT).show()
            Log.e("ProfileFragment", "Error processing image", e)
        }
    }

    private fun updateProfileAvatar(uploadId: Int) {
        // Delete old avatar file before setting new one
        val oldAvatarId = currentAvatarUploadId
        if (oldAvatarId != null && oldAvatarId != uploadId) {
            ApiClient.retrofit.deleteUploadedFile(oldAvatarId)
                .enqueue(object : Callback<StrapiUploadResponse> {
                    override fun onResponse(
                        call: Call<StrapiUploadResponse>,
                        response: Response<StrapiUploadResponse>
                    ) {
                        Log.d("ProfileFragment", "Old avatar deleted successfully")
                        // Continue to update profile with new avatar
                        setNewAvatarInProfile(uploadId)
                    }

                    override fun onFailure(call: Call<StrapiUploadResponse>, t: Throwable) {
                        Log.w("ProfileFragment", "Failed to delete old avatar: ${t.message}")
                        // Continue anyway to set new avatar
                        setNewAvatarInProfile(uploadId)
                    }
                })
        } else {
            // No old avatar to delete, just set the new one
            setNewAvatarInProfile(uploadId)
        }
    }

    private fun setNewAvatarInProfile(uploadId: Int) {
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
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Profilbild löschen")
            .setMessage("Möchtest du dein Profilbild wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                deleteAvatar()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun deleteAvatar() {
        val avatarId = currentAvatarUploadId
        if (avatarId == null) {
            Toast.makeText(requireContext(), "Kein Avatar zum Löschen vorhanden", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // First, delete the file from Strapi
        ApiClient.retrofit.deleteUploadedFile(avatarId)
            .enqueue(object : Callback<StrapiUploadResponse> {
                override fun onResponse(
                    call: Call<StrapiUploadResponse>,
                    response: Response<StrapiUploadResponse>
                ) {
                    // File deleted (or failed), now update profile to remove avatar reference
                    updateProfileToRemoveAvatar()
                }

                override fun onFailure(call: Call<StrapiUploadResponse>, t: Throwable) {
                    Log.w("ProfileFragment", "Failed to delete avatar file: ${t.message}")
                    // Still try to update profile even if file deletion failed
                    updateProfileToRemoveAvatar()
                }
            })
    }

    private fun updateProfileToRemoveAvatar() {
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

    // ===== ACCOUNT DELETION METHODS =====

    private fun updateDeletionUI() {
        if (!currentScheduledDeletionAt.isNullOrEmpty()) {
            // Account is marked for deletion
            deletionWarningCard.visibility = View.VISIBLE
            btnDeleteAccount.visibility = View.GONE

            // Format the deletion date
            val formattedDate = formatDeletionDate(currentScheduledDeletionAt!!)
            deletionWarningText.text = "Dein Konto wird am $formattedDate gelöscht."
        } else {
            // Account is not marked for deletion
            deletionWarningCard.visibility = View.GONE
            btnDeleteAccount.visibility = View.VISIBLE
        }
    }

    private fun formatDeletionDate(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            val date = inputFormat.parse(isoDate.substring(0, 10))
            outputFormat.format(date!!)
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Konto löschen")
            .setMessage("Bist du sicher, dass du dein Konto löschen möchtest?\n\nDein Konto wird in 14 Tagen endgültig gelöscht. Bis dahin kannst du die Löschung jederzeit abbrechen.")
            .setPositiveButton("Konto löschen") { _, _ ->
                requestAccountDeletion()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun requestAccountDeletion() {
        showLoading(true)

        // Calculate deletion date (14 days from now)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 14)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY)
        val deletionDate = dateFormat.format(calendar.time)

        val updateMap = mutableMapOf<String, Any?>(
            "scheduledDeletionAt" to deletionDate
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
                        // Save deletion date and log out
                        AuthManager.saveScheduledDeletionAt(requireContext(), deletionDate)
                        AuthManager.setDeletionWarningShown(requireContext(), false) // Reset warning flag

                        Toast.makeText(
                            requireContext(),
                            "Dein Konto wird in 14 Tagen gelöscht. Du wirst jetzt ausgeloggt.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Log out user
                        logoutUser()
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Löschen: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Error requesting deletion: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Network error requesting deletion", t)
                }
            })
    }

    private fun cancelAccountDeletion() {
        showLoading(true)

        val updateMap = mutableMapOf<String, Any?>(
            "scheduledDeletionAt" to null
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
                        currentScheduledDeletionAt = null
                        AuthManager.saveScheduledDeletionAt(requireContext(), null)
                        updateDeletionUI()
                        Toast.makeText(requireContext(), "Löschung abgebrochen. Dein Konto bleibt aktiv.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Fehler: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Error cancelling deletion: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse<ProfileMeData>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Network error cancelling deletion", t)
                }
            })
    }

    private fun logoutUser() {
        AuthManager.clear(requireContext())

        // Navigate to login screen
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    // ===== AVAILABILITY (SPIELBEREIT) METHODS =====

    private fun loadMyAvailability() {
        availabilityProgress.visibility = View.VISIBLE

        ApiClient.retrofit.getMyAvailability()
            .enqueue(object : Callback<UserAvailabilityResponse> {
                override fun onResponse(
                    call: Call<UserAvailabilityResponse>,
                    response: Response<UserAvailabilityResponse>
                ) {
                    availabilityProgress.visibility = View.GONE
                    if (response.isSuccessful) {
                        val availability = response.body()?.data
                        currentAvailability = availability
                        updateAvailabilityUI(availability)
                    } else {
                        Log.e("ProfileFragment", "Error loading availability: ${response.code()}")
                        updateAvailabilityUI(null)
                    }
                }

                override fun onFailure(call: Call<UserAvailabilityResponse>, t: Throwable) {
                    availabilityProgress.visibility = View.GONE
                    Log.e("ProfileFragment", "Network error loading availability", t)
                    updateAvailabilityUI(null)
                }
            })
    }

    private fun updateAvailabilityUI(availability: UserAvailability?) {
        if (availability != null && availability.isCurrentlyActive()) {
            // Active state
            availabilityInactive.visibility = View.GONE
            availabilityActive.visibility = View.VISIBLE

            // Update card style to show active status
            availabilityCard.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width_active)

            // Format expiry date
            val expiresText = formatExpiryDate(availability.expiresAt)
            availabilityExpiresText.text = "Gültig bis: $expiresText"

            // Hosting preference
            availabilityHostingText.text = availability.getHostingDisplayText()

            // Note (if present)
            if (!availability.note.isNullOrBlank()) {
                availabilityNoteText.text = "\"${availability.note}\""
                availabilityNoteText.visibility = View.VISIBLE
            } else {
                availabilityNoteText.visibility = View.GONE
            }
        } else {
            // Inactive state
            availabilityInactive.visibility = View.VISIBLE
            availabilityActive.visibility = View.GONE
            availabilityCard.strokeWidth = 0
            currentAvailability = null
        }
    }

    private fun formatExpiryDate(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return "Unbekannt"

        return try {
            val instant = Instant.parse(isoDate)
            val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val today = java.time.LocalDate.now()
            val tomorrow = today.plusDays(1)
            val expiryDate = localDateTime.toLocalDate()

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

            when {
                expiryDate == today -> "Heute, ${localDateTime.format(timeFormatter)} Uhr"
                expiryDate == tomorrow -> "Morgen, ${localDateTime.format(timeFormatter)} Uhr"
                else -> "${expiryDate.format(dateFormatter)}, ${localDateTime.format(timeFormatter)} Uhr"
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error parsing expiry date", e)
            isoDate
        }
    }

    private fun showAvailabilityBottomSheet(existingAvailability: UserAvailability? = null) {
        val bottomSheet = AvailabilityBottomSheet.newInstance(existingAvailability) { request ->
            saveAvailability(request)
        }
        bottomSheet.show(childFragmentManager, "availability_bottom_sheet")
    }

    private fun saveAvailability(request: SetAvailabilityRequest) {
        availabilityProgress.visibility = View.VISIBLE

        ApiClient.retrofit.setMyAvailability(request)
            .enqueue(object : Callback<AvailabilityActionResponse> {
                override fun onResponse(
                    call: Call<AvailabilityActionResponse>,
                    response: Response<AvailabilityActionResponse>
                ) {
                    availabilityProgress.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Du bist jetzt als spielbereit markiert!", Toast.LENGTH_SHORT).show()
                        currentAvailability = response.body()?.data
                        updateAvailabilityUI(currentAvailability)
                    } else {
                        val errorMsg = response.body()?.message ?: "Fehler beim Speichern"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Error saving availability: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AvailabilityActionResponse>, t: Throwable) {
                    availabilityProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Network error saving availability", t)
                }
            })
    }

    private fun confirmRemoveAvailability() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Spielbereit-Status entfernen")
            .setMessage("Möchtest du deinen Spielbereit-Status wirklich entfernen?")
            .setPositiveButton("Entfernen") { _, _ ->
                removeAvailability()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun removeAvailability() {
        availabilityProgress.visibility = View.VISIBLE

        ApiClient.retrofit.deleteMyAvailability()
            .enqueue(object : Callback<AvailabilityActionResponse> {
                override fun onResponse(
                    call: Call<AvailabilityActionResponse>,
                    response: Response<AvailabilityActionResponse>
                ) {
                    availabilityProgress.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Spielbereit-Status entfernt", Toast.LENGTH_SHORT).show()
                        currentAvailability = null
                        updateAvailabilityUI(null)
                    } else {
                        Toast.makeText(requireContext(), "Fehler beim Entfernen", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Error removing availability: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AvailabilityActionResponse>, t: Throwable) {
                    availabilityProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Network error removing availability", t)
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
        pushEnabled = null,  // Not available in ProfileData
        pushFrequency = null,
        pushQuietStart = null,
        pushQuietEnd = null,
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
        "pushEnabled" to this.pushEnabled,
        "pushFrequency" to this.pushFrequency,
        "pushQuietStart" to this.pushQuietStart,
        "pushQuietEnd" to this.pushQuietEnd,
        "latitude" to this.latitude,
        "longitude" to this.longitude,
        "cords" to this.cords
    )
}
