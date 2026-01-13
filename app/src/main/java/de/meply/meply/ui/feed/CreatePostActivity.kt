package de.meply.meply.ui.feed

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.BaseDetailActivity
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

class CreatePostActivity : BaseDetailActivity() {

    private lateinit var contentInput: TextInputEditText
    private lateinit var visibilityRadioGroup: RadioGroup
    private lateinit var selectImagesButton: Button
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var imagesContainer: LinearLayout

    private val selectedImages = mutableListOf<SelectedImage>()
    private val maxImages = 4
    private var parentDocumentId: String? = null

    data class SelectedImage(
        val uri: Uri,
        var altText: String = ""
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        handleSelectedImages(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        // Get parent ID if this is a reply
        parentDocumentId = intent.getStringExtra("parentDocumentId")

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        setupDetailToolbar()

        contentInput = findViewById(R.id.postContentInput)
        visibilityRadioGroup = findViewById(R.id.visibilityRadioGroup)
        selectImagesButton = findViewById(R.id.selectImagesButton)
        submitButton = findViewById(R.id.submitPostButton)
        progressBar = findViewById(R.id.createPostProgressBar)
        imagesContainer = findViewById(R.id.selectedImagesContainer)
    }

    private fun setupListeners() {
        selectImagesButton.setOnClickListener {
            if (selectedImages.size >= maxImages) {
                Toast.makeText(
                    this,
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
                this,
                "Nur ${imagesToAdd.size} Bilder hinzugefügt (max. $maxImages)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateImagesUI() {
        imagesContainer.removeAllViews()

        for ((index, selectedImage) in selectedImages.withIndex()) {
            val imageView = LayoutInflater.from(this)
                .inflate(R.layout.item_selected_image, imagesContainer, false)

            val imageViewWidget = imageView.findViewById<ImageView>(R.id.selectedImageView)
            val removeButton = imageView.findViewById<ImageButton>(R.id.removeImageButton)
            val altTextInput = imageView.findViewById<TextInputEditText>(R.id.altTextInput)

            // Load image
            imageViewWidget.setImageURI(selectedImage.uri)

            // Set alt text
            altTextInput.setText(selectedImage.altText)

            // Update alt text on change
            altTextInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    selectedImages[index].altText = s?.toString() ?: ""
                }
            })

            // Remove button
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
            Toast.makeText(this, "Bitte Text eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        val visibility = when (visibilityRadioGroup.checkedRadioButtonId) {
            R.id.visibilityFollower -> "follower"
            else -> "members"
        }

        setLoading(true)

        if (selectedImages.isEmpty()) {
            // No images, create post directly
            submitPost(content, visibility, emptyList())
        } else {
            // Upload images first
            uploadImages { imageIds ->
                if (imageIds != null) {
                    submitPost(content, visibility, imageIds)
                } else {
                    setLoading(false)
                    Toast.makeText(
                        this,
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

            val api = ApiClient.retrofit
            api.uploadImage(body, selectedImage.altText, "post")
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
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Resize if too large
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

            // Compress
            val outputFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)
            var quality = 90

            do {
                val byteArrayOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                val size = byteArrayOutputStream.size()

                if (size <= 2 * 1024 * 1024) { // 2MB
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
                        this@CreatePostActivity,
                        "Beitrag erstellt!",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@CreatePostActivity,
                        "Fehler beim Erstellen: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<CreatePostResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(
                    this@CreatePostActivity,
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
    }
}
