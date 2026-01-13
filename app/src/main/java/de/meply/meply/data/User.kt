package de.meply.meply.data

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    @SerializedName("documentId") val documentId: String?,
    val username: String,
    val email: String,
    val profile: ProfileReference?
)

data class ProfileReference(
    @SerializedName("documentId") val documentId: String?
)
