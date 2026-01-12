package de.meply.meply.auth

import android.content.Context
import android.content.SharedPreferences

object AuthManager {
    // Name für die Standard SharedPreferences-Datei
    private const val PREFS_FILE_NAME = "auth_prefs" // Zurück zum alten Namen oder ein neuer
    private const val KEY_JWT = "jwt"
    private const val KEY_PROFILE_DOCUMENT_ID = "profile_document_id"

    // Hilfsmethode, um die Standard-SharedPreferences zu bekommen
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
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

    fun clear(context: Context) {
        getSharedPreferences(context)
            .edit()
            .clear()
            .apply()
    }
}
