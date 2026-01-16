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

    private var currentFilter: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_markt, container, false)

        recycler = view.findViewById(R.id.recyclerMarkt)
        progress = view.findViewById(R.id.progress)
        empty = view.findViewById(R.id.emptyView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        adapter = MarktAdapter { userSlug ->
            UserProfileActivity.start(requireContext(), userSlug)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            loadMarktplace()
        }

        loadMarktplace()

        return view
    }

    fun showFilterBottomSheet() {
        val bottomSheet = MarktFilterBottomSheet.newInstance(currentFilter)
        bottomSheet.setOnFilterAppliedListener { filter ->
            currentFilter = filter
            loadMarktplace()
        }
        bottomSheet.show(parentFragmentManager, "marktFilter")
    }

    private fun loadMarktplace() {
        showLoading(true)

        ApiClient.retrofit.getMarktplace(page = 1, pageSize = 50, title = currentFilter)
            .enqueue(object : Callback<MarktplaceResponse> {
                override fun onResponse(
                    call: Call<MarktplaceResponse>,
                    response: Response<MarktplaceResponse>
                ) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        val games = response.body()?.results ?: emptyList()

                        if (games.isEmpty()) {
                            empty.text = if (currentFilter != null) {
                                "Keine Angebote für \"$currentFilter\" gefunden."
                            } else {
                                "Keine Angebote gefunden."
                            }
                            empty.visibility = View.VISIBLE
                            adapter.submit(emptyList())
                        } else {
                            empty.visibility = View.GONE
                            adapter.submit(games)
                        }

                        Log.d("MarktFragment", "Loaded ${games.size} games")
                    } else {
                        empty.text = "Laden fehlgeschlagen (${response.code()})"
                        empty.visibility = View.VISIBLE
                        Log.e("MarktFragment", "Error loading marketplace: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<MarktplaceResponse>, t: Throwable) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    empty.text = "Netzwerkfehler: ${t.message}"
                    empty.visibility = View.VISIBLE
                    Log.e("MarktFragment", "Network error loading marketplace", t)
                }
            })
    }

    private fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        if (show) empty.visibility = View.GONE
    }
}
