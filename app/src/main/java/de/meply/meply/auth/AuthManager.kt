package de.meply.meply.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AuthManager {
    private const val PREFS_FILE_NAME = "auth_prefs_encrypted"
    private const val KEY_JWT = "jwt"
    private const val KEY_PROFILE_DOCUMENT_ID = "profile_document_id"
    private const val KEY_PROFILE_ID = "profile_id"
    private const val KEY_USER_DOCUMENT_ID = "user_document_id"
    private const val KEY_SCHEDULED_DELETION_AT = "scheduled_deletion_at"
    private const val KEY_DELETION_WARNING_SHOWN = "deletion_warning_shown"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveJwt(context: Context, jwt: String) {
        getSharedPreferences(context)
            .edit()
            .putString(KEY_JWT, jwt)
            .apply()
    }

    fun getJwt(context: Context): String? {
        return getSharedPreferences(context)
            .getString(KEY_JWT, null)
    }

    fun saveProfileDocumentId(context: Context, documentId: String) {
        getSharedPreferences(context)
            .edit()
            .putString(KEY_PROFILE_DOCUMENT_ID, documentId)
            .apply()
    }

    fun getProfileDocumentId(context: Context): String? {
        return getSharedPreferences(context)
            .getString(KEY_PROFILE_DOCUMENT_ID, null)
    }

    fun saveProfileId(context: Context, id: Int) {
        getSharedPreferences(context)
            .edit()
            .putInt(KEY_PROFILE_ID, id)
            .apply()
    }

    fun getProfileId(context: Context): Int? {
        val prefs = getSharedPreferences(context)
        return if (prefs.contains(KEY_PROFILE_ID)) {
            prefs.getInt(KEY_PROFILE_ID, -1)
        } else {
            null
        }
    }

    fun saveUserDocumentId(context: Context, documentId: String) {
        getSharedPreferences(context)
            .edit()
            .putString(KEY_USER_DOCUMENT_ID, documentId)
            .apply()
    }

    fun getUserDocumentId(context: Context): String? {
        return getSharedPreferences(context)
            .getString(KEY_USER_DOCUMENT_ID, null)
    }

    fun saveScheduledDeletionAt(context: Context, deletionDate: String?) {
        getSharedPreferences(context)
            .edit()
            .putString(KEY_SCHEDULED_DELETION_AT, deletionDate)
            .apply()
    }

    fun getScheduledDeletionAt(context: Context): String? {
        return getSharedPreferences(context)
            .getString(KEY_SCHEDULED_DELETION_AT, null)
    }

    fun setDeletionWarningShown(context: Context, shown: Boolean) {
        getSharedPreferences(context)
            .edit()
            .putBoolean(KEY_DELETION_WARNING_SHOWN, shown)
            .apply()
    }

    fun isDeletionWarningShown(context: Context): Boolean {
        return getSharedPreferences(context)
            .getBoolean(KEY_DELETION_WARNING_SHOWN, false)
    }

    fun clear(context: Context) {
        getSharedPreferences(context)
            .edit()
            .clear()
            .apply()
    }
}
