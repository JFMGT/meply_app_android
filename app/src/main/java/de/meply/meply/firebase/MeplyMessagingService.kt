package de.meply.meply.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.meply.meply.HomeActivity
import de.meply.meply.LoginActivity
import de.meply.meply.R
import de.meply.meply.auth.AuthManager
import de.meply.meply.data.profile.ProfileItem
import de.meply.meply.data.profile.ProfileResponse
import de.meply.meply.network.ApiClient
import de.meply.meply.ui.profile.UserProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MeplyMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MeplyFCM"
        private const val CHANNEL_ID = "meply_notifications"
        private const val CHANNEL_NAME = "Meply Benachrichtigungen"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MeplyMessagingService created")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "=== NEW FCM TOKEN ===")
        Log.d(TAG, "Token: $token")
        Log.d(TAG, "=====================")
        // TODO: Send token to your backend server
        // This token is used to send push notifications to this device
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "=== MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "Message ID: ${message.messageId}")

        // Check if message contains a notification payload
        val notification = message.notification
        if (notification != null) {
            Log.d(TAG, "Notification Title: ${notification.title}")
            Log.d(TAG, "Notification Body: ${notification.body}")
        }

        // Check if message contains a data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${message.data}")
        }

        // Extract elementType from data payload (for deep linking)
        val elementType = message.data["elementType"]
        val elementId = message.data["elementId"]

        // Only show ONE notification - prefer notification payload, fallback to data
        if (notification != null && !notification.title.isNullOrEmpty()) {
            // Use notification payload (from server) with data for deep linking
            showNotification(notification.title, notification.body, elementType, elementId)
        } else if (message.data.isNotEmpty()) {
            // Fallback to data payload only if no notification payload
            handleDataMessage(message.data)
        }

        Log.d(TAG, "========================")
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]

        when (type) {
            "availability" -> handleAvailabilityNotification(data)
            else -> {
                // Default handling for other notification types
                val title = data["title"] ?: "Meply"
                val body = data["body"] ?: data["message"] ?: ""
                val elementType = data["elementType"]
                val elementId = data["elementId"]
                showNotification(title, body, elementType, elementId)
            }
        }
    }

    private fun handleAvailabilityNotification(data: Map<String, String>) {
        val subtype = data["subtype"]
        val profileDocumentId = data["profileDocumentId"]

        if (profileDocumentId.isNullOrEmpty()) {
            Log.e(TAG, "No profileDocumentId in availability notification")
            return
        }

        // Fetch profile data from API
        ApiClient.retrofit.getProfile(profileDocumentId).enqueue(object : Callback<ProfileResponse<ProfileItem>> {
            override fun onResponse(
                call: Call<ProfileResponse<ProfileItem>>,
                response: Response<ProfileResponse<ProfileItem>>
            ) {
                val profileData = response.body()?.data
                val username = profileData?.attributes?.username
                val userSlug = profileData?.attributes?.userslug

                val (title, body) = when (subtype) {
                    "friend" -> {
                        val displayName = username ?: "Ein Freund"
                        "Spielpartner gesucht!" to "$displayName sucht kurzfristig Mitspieler! Bist du dabei?"
                    }
                    "match" -> {
                        "Spielpartner in der Nähe!" to "Jemand aus der Nähe mit einem guten Geschmack sucht spontan Mitspieler! Interessiert?"
                    }
                    else -> {
                        "Spielpartner gesucht!" to "Jemand möchte spielen!"
                    }
                }

                showAvailabilityNotification(title, body, userSlug)
            }

            override fun onFailure(call: Call<ProfileResponse<ProfileItem>>, t: Throwable) {
                Log.e(TAG, "Failed to fetch profile for availability notification", t)
                // Show generic notification as fallback
                val (title, body) = when (subtype) {
                    "friend" -> "Spielpartner gesucht!" to "Ein Freund sucht kurzfristig Mitspieler! Bist du dabei?"
                    "match" -> "Spielpartner in der Nähe!" to "Jemand aus der Nähe mit einem guten Geschmack sucht spontan Mitspieler! Interessiert?"
                    else -> "Spielpartner gesucht!" to "Jemand möchte spielen!"
                }
                showAvailabilityNotification(title, body, null)
            }
        })
    }

    private fun showAvailabilityNotification(title: String, body: String, userSlug: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen von Meply"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Determine target activity based on login state and userSlug availability
        val isLoggedIn = AuthManager.getJwt(this) != null

        val intent = if (isLoggedIn && !userSlug.isNullOrEmpty()) {
            // Open user profile directly
            Intent(this, UserProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("user_slug", userSlug)
            }
        } else if (isLoggedIn) {
            // Open home activity if no userSlug
            Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            // Open login if not logged in
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dice)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        // Show notification with unique ID
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun showNotification(title: String?, body: String?, elementType: String? = null, elementId: String? = null) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen von Meply"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Determine target activity based on login state
        val isLoggedIn = AuthManager.getJwt(this) != null
        val targetClass = if (isLoggedIn) HomeActivity::class.java else LoginActivity::class.java

        // Create intent to open app when notification is clicked
        val intent = Intent(this, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Pass deep linking data
            elementType?.let { putExtra("elementType", it) }
            elementId?.let { putExtra("elementId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code for each notification
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mail)
            .setContentTitle(title ?: "Meply")
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Show notification with unique ID
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }
}
