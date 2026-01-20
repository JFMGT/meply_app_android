package de.meply.meply.ui.uploads

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
import de.meply.meply.R
import de.meply.meply.data.uploads.UserUpload
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ImageViewerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val EXTRA_IMAGE_NAME = "extra_image_name"
        private const val EXTRA_IMAGE_MIME = "extra_image_mime"
        private const val EXTRA_IMAGE_SIZE = "extra_image_size"
        private const val EXTRA_IMAGE_WIDTH = "extra_image_width"
        private const val EXTRA_IMAGE_HEIGHT = "extra_image_height"
        private const val EXTRA_IMAGE_REASON = "extra_image_reason"
        private const val EXTRA_IMAGE_DATE = "extra_image_date"

        fun start(context: Context, upload: UserUpload) {
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URL, upload.file?.previewUrl ?: upload.file?.url)
                putExtra(EXTRA_IMAGE_NAME, upload.file?.name)
                putExtra(EXTRA_IMAGE_MIME, upload.file?.mime)
                putExtra(EXTRA_IMAGE_SIZE, upload.file?.size ?: 0L)
                putExtra(EXTRA_IMAGE_WIDTH, upload.file?.width ?: 0)
                putExtra(EXTRA_IMAGE_HEIGHT, upload.file?.height ?: 0)
                putExtra(EXTRA_IMAGE_REASON, upload.reason)
                putExtra(EXTRA_IMAGE_DATE, upload.createdAt)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var fullscreenImage: ImageView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var btnClose: ImageButton
    private lateinit var imageTitle: TextView
    private lateinit var metaDetails: TextView
    private lateinit var metaReason: TextView
    private lateinit var metaDate: TextView
    private lateinit var topBar: View
    private lateinit var bottomBar: View

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val outputFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)

    private var barsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_image_viewer)

        initializeViews()
        loadImage()
        setupClickListeners()
    }

    private fun initializeViews() {
        fullscreenImage = findViewById(R.id.fullscreen_image)
        loadingSpinner = findViewById(R.id.loading_spinner)
        btnClose = findViewById(R.id.btn_close)
        imageTitle = findViewById(R.id.image_title)
        metaDetails = findViewById(R.id.meta_details)
        metaReason = findViewById(R.id.meta_reason)
        metaDate = findViewById(R.id.meta_date)
        topBar = findViewById(R.id.top_bar)
        bottomBar = findViewById(R.id.bottom_bar)
    }

    private fun loadImage() {
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        val name = intent.getStringExtra(EXTRA_IMAGE_NAME) ?: "Bild"
        val mime = intent.getStringExtra(EXTRA_IMAGE_MIME) ?: "–"
        val size = intent.getLongExtra(EXTRA_IMAGE_SIZE, 0)
        val width = intent.getIntExtra(EXTRA_IMAGE_WIDTH, 0)
        val height = intent.getIntExtra(EXTRA_IMAGE_HEIGHT, 0)
        val reason = intent.getStringExtra(EXTRA_IMAGE_REASON)
        val date = intent.getStringExtra(EXTRA_IMAGE_DATE)

        // Set title
        imageTitle.text = name

        // Set meta details
        val sizeStr = formatFileSize(size)
        val dimensionsStr = if (width > 0 && height > 0) "${width}×${height}" else "–"
        metaDetails.text = "$mime • $sizeStr • $dimensionsStr"

        // Set reason
        metaReason.text = "Grund: ${getLocalizedReason(reason)}"

        // Set date
        metaDate.text = "Hochgeladen: ${formatDate(date)}"

        // Load image
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingSpinner.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingSpinner.visibility = View.GONE
                        return false
                    }
                })
                .into(fullscreenImage)
        } else {
            loadingSpinner.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            finish()
        }

        // Toggle bars on image tap
        fullscreenImage.setOnClickListener {
            toggleBars()
        }
    }

    private fun toggleBars() {
        barsVisible = !barsVisible
        val visibility = if (barsVisible) View.VISIBLE else View.GONE

        topBar.animate()
            .alpha(if (barsVisible) 1f else 0f)
            .setDuration(200)
            .withEndAction {
                if (!barsVisible) topBar.visibility = View.GONE
            }
            .start()

        bottomBar.animate()
            .alpha(if (barsVisible) 1f else 0f)
            .setDuration(200)
            .withEndAction {
                if (!barsVisible) bottomBar.visibility = View.GONE
            }
            .start()

        if (barsVisible) {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes > 0 -> "$bytes B"
            else -> "– KB"
        }
    }

    private fun getLocalizedReason(reason: String?): String {
        return when (reason) {
            "post" -> "Beitrag"
            "avatar" -> "Profilbild"
            "message" -> "Nachricht"
            else -> reason ?: "Unbekannt"
        }
    }

    private fun formatDate(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return "–"
        return try {
            val parsed = inputFormat.parse(isoDate)
            if (parsed != null) outputFormat.format(parsed) else "–"
        } catch (e: Exception) {
            isoDate.substringBefore('T')
        }
    }
}
