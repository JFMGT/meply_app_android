package de.meply.meply.ui.pm

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import de.meply.meply.R
import de.meply.meply.data.messages.*
import de.meply.meply.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConversationActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var messageInput: TextInputEditText
    private lateinit var sendButton: ImageButton
    private lateinit var messageAdapter: MessageAdapter

    private var conversationId: String? = null
    private var partnerName: String? = null
    private var currentUserId: String? = null
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        conversationId = intent.getStringExtra("conversationId")
        partnerName = intent.getStringExtra("partnerName")

        if (conversationId == null) {
            Toast.makeText(this, "Fehler: Konversations-ID fehlt", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSendButton()

        loadMessages()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.conversationRecyclerView)
        swipeRefresh = findViewById(R.id.conversationSwipeRefresh)
        progressBar = findViewById(R.id.conversationProgressBar)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        toolbar.title = partnerName ?: "Konversation"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            messages = messages,
            currentUserId = currentUserId
        )

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // Show latest messages at bottom
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = messageAdapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadMessages()
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        progressBar.visibility = View.VISIBLE

        val api = ApiClient.retrofit
        api.getMessages(conversationId!!).enqueue(object : Callback<MessagesResponse> {
            override fun onResponse(
                call: Call<MessagesResponse>,
                response: Response<MessagesResponse>
            ) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    val messagesResponse = response.body()
                    if (messagesResponse != null) {
                        val messagesList = messagesResponse.data
                        messageAdapter.updateMessages(messagesList)

                        // Scroll to bottom
                        if (messagesList.isNotEmpty()) {
                            recyclerView.scrollToPosition(messagesList.size - 1)
                        }

                        // Set current user ID from first own message
                        if (currentUserId == null) {
                            // This is a simplification - ideally get from user session
                            val firstMessage = messagesList.firstOrNull()
                            if (firstMessage != null) {
                                currentUserId = firstMessage.author.documentId
                            }
                        }

                        Log.d("ConversationActivity", "Loaded ${messagesList.size} messages")
                    }
                } else {
                    Toast.makeText(
                        this@ConversationActivity,
                        "Fehler beim Laden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ConversationActivity", "Error loading messages: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<MessagesResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                Toast.makeText(
                    this@ConversationActivity,
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ConversationActivity", "Network error loading messages", t)
            }
        })
    }

    private fun sendMessage() {
        val messageText = messageInput.text?.toString()?.trim()
        if (messageText.isNullOrEmpty()) {
            Toast.makeText(this, "Bitte eine Nachricht eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        if (messageText.length > 500) {
            Toast.makeText(this, "Nachricht zu lang (max. 500 Zeichen)", Toast.LENGTH_SHORT).show()
            return
        }

        sendButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val request = SendMessageRequest(
            data = SendMessageData(
                content = messageText,
                conversation = ConversationConnect(
                    connect = ConversationId(conversationId!!)
                )
            )
        )

        val api = ApiClient.retrofit
        api.sendMessage(request).enqueue(object : Callback<SendMessageResponse> {
            override fun onResponse(
                call: Call<SendMessageResponse>,
                response: Response<SendMessageResponse>
            ) {
                sendButton.isEnabled = true
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val sendResponse = response.body()
                    if (sendResponse != null) {
                        // Add message to list
                        messageAdapter.addMessage(sendResponse.data)

                        // Scroll to bottom
                        recyclerView.scrollToPosition(messages.size - 1)

                        // Clear input
                        messageInput.text?.clear()

                        Log.d("ConversationActivity", "Message sent successfully")
                    }
                } else {
                    Toast.makeText(
                        this@ConversationActivity,
                        "Fehler beim Senden: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ConversationActivity", "Error sending message: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<SendMessageResponse>, t: Throwable) {
                sendButton.isEnabled = true
                progressBar.visibility = View.GONE

                Toast.makeText(
                    this@ConversationActivity,
                    "Netzwerkfehler: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ConversationActivity", "Network error sending message", t)
            }
        })
    }
}
