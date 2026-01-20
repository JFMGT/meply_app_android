package de.meply.meply.ui.uploads

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import de.meply.meply.BaseDetailActivity
import de.meply.meply.R
import de.meply.meply.data.uploads.DeleteUploadResponse
import de.meply.meply.data.uploads.UserUpload
import de.meply.meply.data.uploads.UserUploadsResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyUploadsActivity : BaseDetailActivity() {

    companion object {
        private const val TAG = "MyUploadsActivity"
        private const val PAGE_SIZE = 24

        fun start(context: Context) {
            val intent = Intent(context, MyUploadsActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var uploadsStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var uploadsRecycler: RecyclerView
    private lateinit var paginationContainer: View
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var pageIndicator: TextView

    private lateinit var adapter: UserUploadsAdapter

    private var currentPage = 1
    private var totalPages = 1
    private var totalItems = 0
    private val uploads = mutableListOf<UserUpload>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_uploads)

        setupToolbar()
        initializeViews()
        setupAdapter()
        setupPagination()
        loadUploads(1)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Meine Bilder"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializeViews() {
        uploadsStats = findViewById(R.id.uploads_stats)
        loadingProgress = findViewById(R.id.loading_progress)
        emptyCard = findViewById(R.id.empty_card)
        uploadsRecycler = findViewById(R.id.uploads_recycler)
        paginationContainer = findViewById(R.id.pagination_container)
        btnPrevPage = findViewById(R.id.btn_prev_page)
        btnNextPage = findViewById(R.id.btn_next_page)
        pageIndicator = findViewById(R.id.page_indicator)
    }

    private fun setupAdapter() {
        adapter = UserUploadsAdapter(
            onImageClick = { upload -> openImageViewer(upload) },
            onDeleteClick = { upload -> confirmDeleteUpload(upload) }
        )

        // Use GridLayoutManager with 2 columns
        val gridLayoutManager = GridLayoutManager(this, 2)
        uploadsRecycler.layoutManager = gridLayoutManager
        uploadsRecycler.adapter = adapter
    }

    private fun setupPagination() {
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                loadUploads(currentPage - 1)
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                loadUploads(currentPage + 1)
            }
        }
    }

    private fun loadUploads(page: Int) {
        loadingProgress.visibility = View.VISIBLE
        emptyCard.visibility = View.GONE
        uploadsRecycler.visibility = View.GONE
        paginationContainer.visibility = View.GONE

        ApiClient.retrofit.getMyUploads(page = page, pageSize = PAGE_SIZE)
            .enqueue(object : Callback<UserUploadsResponse> {
                override fun onResponse(
                    call: Call<UserUploadsResponse>,
                    response: Response<UserUploadsResponse>
                ) {
                    loadingProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        val body = response.body()
                        val items = body?.results ?: emptyList()
                        val pagination = body?.pagination

                        currentPage = pagination?.page ?: page
                        totalPages = pagination?.pageCount ?: 1
                        totalItems = pagination?.total ?: items.size

                        uploads.clear()
                        uploads.addAll(items)
                        updateUI()
                    } else {
                        Log.e(TAG, "Error loading uploads: ${response.code()}")
                        Toast.makeText(
                            this@MyUploadsActivity,
                            "Fehler beim Laden der Bilder",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI()
                    }
                }

                override fun onFailure(call: Call<UserUploadsResponse>, t: Throwable) {
                    loadingProgress.visibility = View.GONE
                    Log.e(TAG, "Error loading uploads", t)
                    Toast.makeText(
                        this@MyUploadsActivity,
                        "Netzwerkfehler: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    updateUI()
                }
            })
    }

    private fun updateUI() {
        if (uploads.isEmpty()) {
            emptyCard.visibility = View.VISIBLE
            uploadsRecycler.visibility = View.GONE
            paginationContainer.visibility = View.GONE
            uploadsStats.text = "Keine Bilder"
        } else {
            emptyCard.visibility = View.GONE
            uploadsRecycler.visibility = View.VISIBLE

            // Update stats
            uploadsStats.text = "$totalItems Bilder"

            // Update adapter
            adapter.submitList(uploads.toList())

            // Update pagination
            if (totalPages > 1) {
                paginationContainer.visibility = View.VISIBLE
                pageIndicator.text = "Seite $currentPage von $totalPages"
                btnPrevPage.isEnabled = currentPage > 1
                btnNextPage.isEnabled = currentPage < totalPages
            } else {
                paginationContainer.visibility = View.GONE
            }
        }
    }

    private fun openImageViewer(upload: UserUpload) {
        ImageViewerActivity.start(this, upload)
    }

    private fun confirmDeleteUpload(upload: UserUpload) {
        AlertDialog.Builder(this, R.style.Theme_Meply_AlertDialog)
            .setTitle("Bild löschen")
            .setMessage("Willst du dieses Bild wirklich löschen?\n\nHinweis: Der dazugehörige Beitrag bleibt bestehen.")
            .setPositiveButton("Löschen") { _, _ ->
                deleteUpload(upload)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun deleteUpload(upload: UserUpload) {
        loadingProgress.visibility = View.VISIBLE

        ApiClient.retrofit.deleteUserUpload(upload.id)
            .enqueue(object : Callback<DeleteUploadResponse> {
                override fun onResponse(
                    call: Call<DeleteUploadResponse>,
                    response: Response<DeleteUploadResponse>
                ) {
                    loadingProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MyUploadsActivity,
                            "Bild gelöscht",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Remove from local list
                        uploads.removeAll { it.id == upload.id }

                        if (uploads.isEmpty() && currentPage > 1) {
                            // Go back to previous page if current page is empty
                            loadUploads(currentPage - 1)
                        } else if (uploads.isEmpty()) {
                            // Reload first page
                            loadUploads(1)
                        } else {
                            // Update UI with remaining items
                            totalItems--
                            updateUI()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error deleting upload: ${response.code()} - $errorBody")
                        Toast.makeText(
                            this@MyUploadsActivity,
                            "Fehler beim Löschen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<DeleteUploadResponse>, t: Throwable) {
                    loadingProgress.visibility = View.GONE
                    Log.e(TAG, "Error deleting upload", t)
                    Toast.makeText(
                        this@MyUploadsActivity,
                        "Netzwerkfehler beim Löschen",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
