package de.meply.meply.firebase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import de.meply.meply.BuildConfig

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    /**
     * Check if Firebase is available (only in release builds)
     */
    private fun isFirebaseAvailable(): Boolean {
        return !BuildConfig.DEBUG
    }

    /**
     * Get the current FCM token asynchronously
     */
    fun getToken(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit = {}) {
        if (!isFirebaseAvailable()) {
            Log.d(TAG, "Firebase not available in debug build")
            onFailure(Exception("Firebase not available in debug build"))
            return
        }

        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Log.d(TAG, "FCM Token: $token")
                    onSuccess(token)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get FCM token", exception)
                    onFailure(exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not initialized", e)
            onFailure(e)
        }
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }
    }

    /**
     * Subscribe to a topic (e.g., "general", "events", "news")
     */
    fun subscribeToTopic(topic: String) {
        if (!isFirebaseAvailable()) {
            Log.d(TAG, "Firebase not available in debug build - skip subscribe to $topic")
            return
        }

        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener {
                    Log.d(TAG, "Subscribed to topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to subscribe to topic: $topic", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not initialized", e)
        }
    }

    /**
     * Unsubscribe from a topic
     */
    fun unsubscribeFromTopic(topic: String) {
        if (!isFirebaseAvailable()) {
            Log.d(TAG, "Firebase not available in debug build - skip unsubscribe from $topic")
            return
        }

        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnSuccessListener {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not initialized", e)
        }
    }
}
