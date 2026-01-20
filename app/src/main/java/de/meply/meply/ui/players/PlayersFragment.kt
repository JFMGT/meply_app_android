package de.meply.meply.ui.players

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.players.PlayerMatch
import de.meply.meply.network.ApiClient
import de.meply.meply.ui.profile.UserProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlayersFragment : Fragment() {

    companion object {
        private const val TAG = "PlayersFragment"
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var playersStats: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyCard: MaterialCardView
    private lateinit var playersRecycler: RecyclerView

    private lateinit var adapter: PlayersAdapter
    private val players = mutableListOf<PlayerMatch>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupRecyclerView()
        setupListeners()
        loadPlayers()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        playersStats = view.findViewById(R.id.players_stats)
        loadingProgress = view.findViewById(R.id.loading_progress)
        emptyCard = view.findViewById(R.id.empty_card)
        playersRecycler = view.findViewById(R.id.players_recycler)
    }

    private fun setupRecyclerView() {
        adapter = PlayersAdapter { match ->
            onPlayerClick(match)
        }

        playersRecycler.layoutManager = LinearLayoutManager(requireContext())
        playersRecycler.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            loadPlayers()
        }
    }

    fun loadPlayers() {
        val profileId = AuthManager.getProfileId(requireContext())
        if (profileId == null) {
            playersStats.text = "Nicht angemeldet"
            swipeRefresh.isRefreshing = false
            return
        }

        if (!swipeRefresh.isRefreshing) {
            loadingProgress.visibility = View.VISIBLE
        }
        playersStats.text = "Lade Mitspieler..."

        ApiClient.retrofit.getBestMatches(profileId, 50).enqueue(object : Callback<List<PlayerMatch>> {
            override fun onResponse(
                call: Call<List<PlayerMatch>>,
                response: Response<List<PlayerMatch>>
            ) {
                if (!isAdded) return

                swipeRefresh.isRefreshing = false
                loadingProgress.visibility = View.GONE

                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()
                    players.clear()
                    players.addAll(data)
                    adapter.submitList(players.toList())
                    updateUI()
                } else {
                    playersStats.text = "Fehler beim Laden (${response.code()})"
                    Log.e(TAG, "Error loading players: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<PlayerMatch>>, t: Throwable) {
                if (!isAdded) return

                swipeRefresh.isRefreshing = false
                loadingProgress.visibility = View.GONE
                playersStats.text = "Netzwerkfehler"
                Log.e(TAG, "Network error loading players", t)
            }
        })
    }

    private fun updateUI() {
        val count = players.size
        playersStats.text = when (count) {
            0 -> "Keine Mitspieler gefunden"
            1 -> "1 Mitspieler in deiner Nähe"
            else -> "$count Mitspieler in deiner Nähe"
        }

        if (count == 0) {
            emptyCard.visibility = View.VISIBLE
            playersRecycler.visibility = View.GONE
        } else {
            emptyCard.visibility = View.GONE
            playersRecycler.visibility = View.VISIBLE
        }
    }

    private fun onPlayerClick(match: PlayerMatch) {
        val userslug = match.otherProfile?.userslug
        if (userslug != null) {
            UserProfileActivity.start(requireContext(), userslug)
        }
    }
}
