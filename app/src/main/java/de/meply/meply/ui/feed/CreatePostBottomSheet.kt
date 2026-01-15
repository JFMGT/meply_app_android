package de.meply.meply.ui.feed

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.feed.*
import de.meply.meply.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class CreatePostBottomSheet : BottomSheetDialogFragment() {

    private lateinit var contentInput: TextInputEditText
    private lateinit var visibilityRadioGroup: RadioGroup
    private lateinit var selectImagesButton: Button
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var imagesContainer: LinearLayout
    private lateinit var closeButton: ImageButton

    private val selectedImages = mutableListOf<SelectedImage>()
    private val maxImages = 4
    private var parentDocumentId: String? = null
    private var onPostCreated: (() -> Unit)? = null

    data class SelectedImage(
        val uri: Uri,
        var altText: String = ""
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        handleSelectedImages(uris)
    }

    companion object {
        private const val ARG_PARENT_ID = "parentDocumentId"

        fun newInstance(parentDocumentId: String? = null): CreatePostBottomSheet {
            return CreatePostBottomSheet().apply {
                arguments = Bundle().apply {
                    parentDocumentId?.let { putString(ARG_PARENT_ID, it) }
                }
            }
        }
    }

    fun setOnPostCreatedListener(listener: () -> Unit) {
        onPostCreated = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentDocumentId = arguments?.getString(ARG_PARENT_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                // Disable drag to dismiss
                behavior.isDraggable = false
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupListeners()
    }

    private fun initializeViews(view: View) {
        contentInput = view.findViewById(R.id.postContentInput)
        visibilityRadioGroup = view.findViewById(R.id.visibilityRadioGroup)
        selectImagesButton = view.findViewById(R.id.selectImagesButton)
        submitButton = view.findViewById(R.id.submitPostButton)
        progressBar = view.findViewById(R.id.createPostProgressBar)
        imagesContainer = view.findViewById(R.id.selectedImagesContainer)
        closeButton = view.findViewById(R.id.closeButton)

        // Update hint text based on whether this is a reply
        if (parentDocumentId != null) {
            contentInput.hint = "Deine Antwort..."
            submitButton.text = "Antworten"
        }
    }

    private fun setupListeners() {
        closeButton.setOnClickListener {
            dismiss()
        }

        selectImagesButton.setOnClickListener {
            if (selectedImages.size >= maxImages) {
                Toast.makeText(
                    requireContext(),
                    "Maximal $maxImages Bilder erlaubt",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }

        submitButton.setOnClickListener {
            createPost()
        }
    }

    private fun handleSelectedImages(uris: List<Uri>) {
        val remainingSlots = maxImages - selectedImages.size
        val imagesToAdd = uris.take(remainingSlots)

        for (uri in imagesToAdd) {
            selectedImages.add(SelectedImage(uri))
        }

        updateImagesUI()

        if (imagesToAdd.size < uris.size) {
            Toast.makeText(
                requireContext(),
                "Nur ${imagesToAdd.size} Bilder hinzugefügt (max. $maxImages)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateImagesUI() {
        imagesContainer.removeAllViews()

        for ((index, selectedImage) in selectedImages.withIndex()) {
            val imageView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_selected_image, imagesContainer, false)

            val imageViewWidget = imageView.findViewById<ImageView>(R.id.selectedImageView)
            val removeButton = imageView.findViewById<ImageButton>(R.id.removeImageButton)
            val altTextInput = imageView.findViewById<TextInputEditText>(R.id.altTextInput)

            imageViewWidget.setImageURI(selectedImage.uri)
            altTextInput.setText(selectedImage.altText)

            altTextInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    selectedImages[index].altText = s?.toString() ?: ""
                }
            })

            removeButton.setOnClickListener {
                selectedImages.removeAt(index)
                updateImagesUI()
            }

            imagesContainer.addView(imageView)
        }
    }

    private fun createPost() {
        val content = contentInput.text?.toString()?.trim()
        if (content.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Bitte Text eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        val visibility = when (visibilityRadioGroup.checkedRadioButtonId) {
            R.id.visibilityFollower -> "follower"
            else -> "members"
        }

        setLoading(true)

        if (selectedImages.isEmpty()) {
            submitPost(content, visibility, emptyList())
        } else {
            uploadImages { imageIds ->
                if (imageIds != null) {
                    submitPost(content, visibility, imageIds)
                } else {
                    setLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Hochladen der Bilder",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun uploadImages(callback: (List<Int>?) -> Unit) {
        val uploadedIds = mutableListOf<Int>()
        var uploadedCount = 0
        val totalImages = selectedImages.size

        for (selectedImage in selectedImages) {
            val compressedFile = compressImage(selectedImage.uri)
            if (compressedFile == null) {
                callback(null)
                return
            }

            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "files",
                compressedFile.name,
                requestFile
            )

            val altPart = okhttp3.RequestBody.Companion.create(
                "text/plain".toMediaTypeOrNull(),
                selectedImage.altText
            )
            val purposePart = okhttp3.RequestBody.Companion.create(
                "text/plain".toMediaTypeOrNull(),
                "post"
            )

            val api = ApiClient.retrofit
            api.uploadImage(body, altPart, purposePart)
                .enqueue(object : Callback<ImageUploadResponse> {
                    override fun onResponse(
                        call: Call<ImageUploadResponse>,
                        response: Response<ImageUploadResponse>
                    ) {
                        if (response.isSuccessful) {
                            val uploadResponse = response.body()
                            val imageId = uploadResponse?.data?.file
                                ?: uploadResponse?.uploads?.firstOrNull()?.uploadId

                            if (imageId != null) {
                                uploadedIds.add(imageId)
                            }
                        }

                        uploadedCount++
                        if (uploadedCount == totalImages) {
                            if (uploadedIds.size == totalImages) {
                                callback(uploadedIds)
                            } else {
                                callback(null)
                            }
                        }
                    }

                    override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                        Log.e("CreatePost", "Image upload failed", t)
                        callback(null)
                    }
                })
        }
    }

    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxDimension = 1920
            val scale = if (bitmap.width > bitmap.height) {
                maxDimension.toFloat() / bitmap.width
            } else {
                maxDimension.toFloat() / bitmap.height
            }

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputFile = File(requireContext().cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)
            var quality = 90

            do {
                val byteArrayOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                val size = byteArrayOutputStream.size()

                if (size <= 2 * 1024 * 1024) {
                    outputStream.write(byteArrayOutputStream.toByteArray())
                    break
                }

                quality -= 10
            } while (quality >= 10)

            outputStream.close()
            scaledBitmap.recycle()
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }

            outputFile
        } catch (e: Exception) {
            Log.e("CreatePost", "Image compression failed", e)
            null
        }
    }

    private fun submitPost(content: String, visibility: String, imageIds: List<Int>) {
        val parentConnect = if (parentDocumentId != null) {
            PostParentConnect(PostParentDocumentId(parentDocumentId!!))
        } else {
            null
        }

        val request = CreatePostRequest(
            data = CreatePostData(
                content = content,
                image = imageIds,
                visibility = visibility,
                parent = parentConnect
            )
        )

        val api = ApiClient.retrofit
        api.createPost(request).enqueue(object : Callback<CreatePostResponse> {
            override fun onResponse(
                call: Call<CreatePostResponse>,
                response: Response<CreatePostResponse>
            ) {
                setLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        if (parentDocumentId != null) "Antwort gesendet!" else "Beitrag erstellt!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onPostCreated?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<CreatePostResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("CreatePost", "Post creation failed", t)
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !loading
        selectImagesButton.isEnabled = !loading
        contentInput.isEnabled = !loading
        closeButton.isEnabled = !loading
        isCancelable = !loading
    }
}
