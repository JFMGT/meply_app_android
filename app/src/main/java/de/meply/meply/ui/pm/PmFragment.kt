package de.meply.meply.ui.pm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.meply.meply.R
import de.meply.meply.data.messages.Conversation
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PmFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var conversationAdapter: ConversationAdapter

    private val conversations = mutableListOf<Conversation>()
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_pm, container, false)

        recyclerView = view.findViewById(R.id.pmRecyclerView)
        swipeRefresh = view.findViewById(R.id.pmSwipeRefresh)
        progressBar = view.findViewById(R.id.pmProgressBar)
        emptyState = view.findViewById(R.id.pmEmptyState)

        setupRecyclerView()
        setupSwipeRefresh()

        // Load current user ID (from session/preferences)
        // For now, we'll get it from the API call
        loadConversations()

        return view
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(
            conversations = conversations,
            onConversationClick = { conversation -> openConversation(conversation) },
            currentUserId = currentUserId
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = conversationAdapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadConversations()
        }
    }

    private fun loadConversations() {
        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        val api = ApiClient.retrofit
        api.getConversations().enqueue(object : Callback<List<Conversation>> {
            override fun onResponse(
                call: Call<List<Conversation>>,
                response: Response<List<Conversation>>
            ) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val conversationsList = response.body()
                    if (conversationsList != null) {
                        if (conversationsList.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            emptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            conversationAdapter.updateConversations(conversationsList)
                        }

                        Log.d("PmFragment", "Loaded ${conversationsList.size} conversations")
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Fehler beim Laden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("PmFragment", "Error loading conversations: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Conversation>>, t: Throwable) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                Toast.makeText(
                    requireContext(),
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("PmFragment", "Network error loading conversations", t)
            }
        })
    }

    private fun openConversation(conversation: Conversation) {
        val intent = Intent(requireContext(), ConversationActivity::class.java)
        intent.putExtra("conversationId", conversation.documentId)
        intent.putExtra("partnerName", getPartnerName(conversation))
        startActivity(intent)
    }

    private fun getPartnerName(conversation: Conversation): String {
        val partner = conversation.participants.firstOrNull {
            it.documentId != currentUserId
        } ?: conversation.participants.firstOrNull()

        return partner?.username ?: "Unbekannt"
    }

    override fun onResume() {
        super.onResume()
        // Refresh conversations when returning to this fragment
        loadConversations()
    }
}
