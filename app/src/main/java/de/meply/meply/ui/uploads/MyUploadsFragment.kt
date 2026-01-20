package de.meply.meply.ui.uploads

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import de.meply.meply.R
import de.meply.meply.data.uploads.DeleteUploadResponse
import de.meply.meply.data.uploads.UserUpload
import de.meply.meply.data.uploads.UserUploadsResponse
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyUploadsFragment : Fragment() {

    companion object {
        private const val TAG = "MyUploadsFragment"
        private const val PAGE_SIZE = 24
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var uploadsStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var uploadsRecycler: RecyclerView
    private lateinit var statusCard: MaterialCardView
    private lateinit var statusSpinner: ProgressBar
    private lateinit var statusText: TextView

    private lateinit var adapter: UserUploadsAdapter

    private val uploads = mutableListOf<UserUpload>()
    private var currentPage = 1
    private var totalUploads = 0
    private var isLoading = false
    private var hasMorePages = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_uploads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupAdapter()
        setupScrollListener()
        loadUploads(resetList = true)
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        nestedScrollView = view.findViewById(R.id.nestedScrollView)
        uploadsStats = view.findViewById(R.id.uploads_stats)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        uploadsRecycler = view.findViewById(R.id.uploads_recycler)
        statusCard = view.findViewById(R.id.status_card)
        statusSpinner = view.findViewById(R.id.status_spinner)
        statusText = view.findViewById(R.id.status_text)

        swipeRefresh.setOnRefreshListener {
            loadUploads(resetList = true)
        }
    }

    private fun setupAdapter() {
        adapter = UserUploadsAdapter(
            onImageClick = { upload -> openImageViewer(upload) },
            onDeleteClick = { upload -> confirmDeleteUpload(upload) }
        )

        // Use GridLayoutManager with 2 columns
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        uploadsRecycler.layoutManager = gridLayoutManager
        uploadsRecycler.adapter = adapter
    }

    private fun setupScrollListener() {
        nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            // Only check when scrolling down
            if (scrollY > oldScrollY && hasMorePages && !isLoading) {
                val contentHeight = v.getChildAt(0).measuredHeight
                val scrollViewHeight = v.measuredHeight
                val scrollPosition = scrollY + scrollViewHeight

                // Load more when within 200px of the bottom
                if (scrollPosition >= contentHeight - 200) {
                    loadUploads(resetList = false)
                }
            }
        }
    }

    private fun loadUploads(resetList: Boolean) {
        if (isLoading) return
        isLoading = true

        if (resetList) {
            currentPage = 1
            hasMorePages = true
            uploads.clear()
            loadingProgress.visibility = View.VISIBLE
            emptyCard.visibility = View.GONE
            uploadsRecycler.visibility = View.GONE
            statusCard.visibility = View.GONE
        } else {
            // Loading more - show status card with spinner
            statusCard.visibility = View.VISIBLE
            statusSpinner.visibility = View.VISIBLE
            statusText.text = "Es wird versucht, weitere Einträge zu laden..."
        }

        ApiClient.retrofit.getMyUploads(page = currentPage, pageSize = PAGE_SIZE)
            .enqueue(object : Callback<UserUploadsResponse> {
                override fun onResponse(
                    call: Call<UserUploadsResponse>,
                    response: Response<UserUploadsResponse>
                ) {
                    if (!isAdded) return

                    isLoading = false
                    loadingProgress.visibility = View.GONE
                    statusSpinner.visibility = View.GONE
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        val body = response.body()
                        val newUploads = body?.results ?: emptyList()
                        val pagination = body?.pagination

                        totalUploads = pagination?.total ?: newUploads.size
                        val pageCount = pagination?.pageCount ?: 1
                        hasMorePages = currentPage < pageCount

                        uploads.addAll(newUploads)
                        currentPage++

                        updateUI()
                    } else {
                        Log.e(TAG, "Error loading uploads: ${response.code()}")
                        Toast.makeText(requireContext(), "Fehler beim Laden: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UserUploadsResponse>, t: Throwable) {
                    if (!isAdded) return

                    isLoading = false
                    loadingProgress.visibility = View.GONE
                    statusSpinner.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    Log.e(TAG, "Error loading uploads: ${t.message}", t)
                    Toast.makeText(requireContext(), "Fehler: ${t.message}", Toast.LENGTH_LONG).show()

                    // Show error in status card
                    if (uploads.isNotEmpty()) {
                        statusCard.visibility = View.VISIBLE
                        statusText.text = "Fehler beim Laden weiterer Einträge"
                    }
                }
            })
    }

    private fun updateUI() {
        if (uploads.isEmpty()) {
            emptyCard.visibility = View.VISIBLE
            uploadsRecycler.visibility = View.GONE
            statusCard.visibility = View.GONE
            uploadsStats.text = "Keine Bilder"
        } else {
            emptyCard.visibility = View.GONE
            uploadsRecycler.visibility = View.VISIBLE

            uploadsStats.text = "$totalUploads Bilder"

            adapter.submitList(uploads.toList())

            // Update status card
            statusCard.visibility = View.VISIBLE
            statusSpinner.visibility = View.GONE
            if (!hasMorePages) {
                // All loaded
                statusText.text = "${uploads.size} von $totalUploads Bildern geladen - Keine weiteren Einträge"
            } else {
                // More available
                statusText.text = "Scrolle nach unten für weitere Einträge"
            }
        }
    }

    private fun openImageViewer(upload: UserUpload) {
        ImageViewerActivity.start(requireContext(), upload)
    }

    private fun confirmDeleteUpload(upload: UserUpload) {
        AlertDialog.Builder(requireContext(), R.style.Theme_Meply_AlertDialog)
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
                    if (!isAdded) return

                    loadingProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Bild gelöscht", Toast.LENGTH_SHORT).show()

                        // Remove from local list
                        uploads.removeAll { it.id == upload.id }
                        totalUploads--

                        if (uploads.isEmpty()) {
                            // Reload first page
                            loadUploads(resetList = true)
                        } else {
                            // Update UI with remaining items
                            updateUI()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error deleting upload: ${response.code()} - $errorBody")
                        Toast.makeText(requireContext(), "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<DeleteUploadResponse>, t: Throwable) {
                    if (!isAdded) return

                    loadingProgress.visibility = View.GONE
                    Log.e(TAG, "Error deleting upload", t)
                    Toast.makeText(requireContext(), "Netzwerkfehler beim Löschen", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun refreshUploads() {
        loadUploads(resetList = true)
    }
}
