package de.meply.meply

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.meply.meply.data.meeting.Meeting
import de.meply.meply.data.meeting.MeetingsResponse
import de.meply.meply.network.ApiClient
import de.meply.meply.auth.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GesucheFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var adapter: GesucheAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gesuche, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        setupRecyclerView()
        setupSwipeGestures()

        swipeRefresh.setOnRefreshListener {
            loadMeetings()
        }

        loadMeetings()
    }

    private fun setupRecyclerView() {
        adapter = GesucheAdapter(
            onCreateMeetingClick = { openCreateMeeting() }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSwipeGestures() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val deleteBackground = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.error))
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Don't allow swiping on header
                if (adapter.isHeader(viewHolder.adapterPosition)) {
                    return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val meeting = adapter.getMeetingAt(position)

                if (meeting != null && direction == ItemTouchHelper.LEFT) {
                    confirmRemoveMeeting(meeting, position)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2

                if (dX < 0) {
                    // Swiping left - delete
                    deleteBackground.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteBackground.draw(c)

                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.setTint(ContextCompat.getColor(requireContext(), R.color.white))
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun loadMeetings() {
        progressBar.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE

        val profileId = AuthManager.getProfileDocumentId(requireContext())
        if (profileId == null) {
            Toast.makeText(requireContext(), "Nicht angemeldet", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            return
        }

        android.util.Log.d("GesucheFragment", "Loading meetings for profile: $profileId")

        ApiClient.retrofit.getUserMeetings(
            authorDocumentId = profileId,
            dateIsNull = true,
            dateIsGte = "1984-05-05" // Magic date to show all user meetings
        ).enqueue(object : Callback<MeetingsResponse> {
            override fun onResponse(call: Call<MeetingsResponse>, response: Response<MeetingsResponse>) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val meetings = response.body()?.data ?: emptyList()
                    android.util.Log.d("GesucheFragment", "Loaded ${meetings.size} meetings")

                    adapter.submitList(meetings)

                    if (meetings.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                    } else {
                        emptyStateText.visibility = View.GONE
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("GesucheFragment", "Error: ${response.code()} - $errorBody")
                    Toast.makeText(requireContext(), "Fehler beim Laden: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MeetingsResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmRemoveMeeting(meeting: Meeting, position: Int) {
        AlertDialog.Builder(requireContext(), R.style.Theme_Meply_AlertDialog)
            .setTitle("Gesuch löschen")
            .setMessage("Möchtest du \"${meeting.title}\" wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                deleteMeeting(meeting)
            }
            .setNegativeButton("Abbrechen") { _, _ ->
                // Restore the item
                adapter.notifyItemChanged(position)
            }
            .setOnCancelListener {
                // Restore the item if dialog is cancelled
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun deleteMeeting(meeting: Meeting) {
        val documentId = meeting.documentId ?: return

        progressBar.visibility = View.VISIBLE

        ApiClient.retrofit.deleteMeeting(documentId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Gesuch gelöscht", Toast.LENGTH_SHORT).show()
                    loadMeetings()
                } else {
                    Toast.makeText(requireContext(), "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Netzwerkfehler: ${t.message}", Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged()
            }
        })
    }

    private fun openCreateMeeting() {
        val intent = Intent(requireContext(), CreateMeetingActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadMeetings()
    }
}
