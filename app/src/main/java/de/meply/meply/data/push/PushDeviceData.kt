package de.meply.meply.data.push

import com.google.gson.annotations.SerializedName

/**
 * Request to register a push device token
 */
data class RegisterPushDeviceRequest(
    @SerializedName("token") val token: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("deviceName") val deviceName: String? = null
)

/**
 * Request to unregister a push device token
 */
data class UnregisterPushDeviceRequest(
    @SerializedName("token") val token: String
)

/**
 * Response from register/unregister endpoints
 */
data class PushDeviceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("id") val id: Int? = null
)
