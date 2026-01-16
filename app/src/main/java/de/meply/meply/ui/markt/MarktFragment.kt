package de.meply.meply.ui.markt

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.meply.meply.R
import de.meply.meply.data.markt.MarktGame
import de.meply.meply.data.markt.MarktplaceResponse
import de.meply.meply.network.ApiClient
import de.meply.meply.ui.profile.UserProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MarktFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: MarktAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var currentFilter: String? = null
    private var currentPage = 1
    private val pageSize = 20
    private var isLoading = false
    private var hasMorePages = true
    private val allGames = mutableListOf<MarktGame>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_markt, container, false)

        recycler = view.findViewById(R.id.recyclerMarkt)
        progress = view.findViewById(R.id.progress)
        empty = view.findViewById(R.id.emptyView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        adapter = MarktAdapter { userSlug ->
            UserProfileActivity.start(requireContext(), userSlug)
        }

        layoutManager = LinearLayoutManager(requireContext())
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        // Add scroll listener for pagination
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) { // Scrolling down
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                            loadMoreMarktplace()
                        }
                    }
                }
            }
        })

        swipeRefresh.setOnRefreshListener {
            resetAndLoad()
        }

        loadMarktplace()

        return view
    }

    fun showFilterBottomSheet() {
        val bottomSheet = MarktFilterBottomSheet.newInstance(currentFilter)
        bottomSheet.setOnFilterAppliedListener { filter ->
            currentFilter = filter
            resetAndLoad()
        }
        bottomSheet.show(parentFragmentManager, "marktFilter")
    }

    private fun resetAndLoad() {
        currentPage = 1
        hasMorePages = true
        allGames.clear()
        loadMarktplace()
    }

    private fun loadMarktplace() {
        if (isLoading) return
        isLoading = true

        if (currentPage == 1) {
            showLoading(true)
        }

        ApiClient.retrofit.getMarktplace(page = currentPage, pageSize = pageSize, title = currentFilter)
            .enqueue(object : Callback<MarktplaceResponse> {
                override fun onResponse(
                    call: Call<MarktplaceResponse>,
                    response: Response<MarktplaceResponse>
                ) {
                    isLoading = false
                    showLoading(false)
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        val games = response.body()?.results ?: emptyList()
                        val pagination = response.body()?.pagination

                        // Check if there are more pages
                        val totalPages = pagination?.pageCount ?: 1
                        hasMorePages = currentPage < totalPages

                        if (currentPage == 1) {
                            allGames.clear()
                        }
                        allGames.addAll(games)

                        if (allGames.isEmpty()) {
                            empty.text = if (currentFilter != null) {
                                "Keine Angebote für \"$currentFilter\" gefunden."
                            } else {
                                "Keine Angebote gefunden."
                            }
                            empty.visibility = View.VISIBLE
                            adapter.submit(emptyList())
                        } else {
                            empty.visibility = View.GONE
                            adapter.submit(allGames.toList())
                        }

                        Log.d("MarktFragment", "Loaded page $currentPage with ${games.size} games, total: ${allGames.size}, hasMore: $hasMorePages")
                    } else {
                        if (currentPage == 1) {
                            empty.text = "Laden fehlgeschlagen (${response.code()})"
                            empty.visibility = View.VISIBLE
                        }
                        Log.e("MarktFragment", "Error loading marketplace: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarktplaceResponse>, t: Throwable) {
                    isLoading = false
                    showLoading(false)
                    swipeRefresh.isRefreshing = false

                    if (currentPage == 1) {
                        empty.text = "Netzwerkfehler: ${t.message}"
                        empty.visibility = View.VISIBLE
                    }
                    Log.e("MarktFragment", "Network error loading marketplace", t)
                }
            })
    }

    private fun loadMoreMarktplace() {
        currentPage++
        loadMarktplace()
    }

    private fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        if (show) empty.visibility = View.GONE
    }
}
