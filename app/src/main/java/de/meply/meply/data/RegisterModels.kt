package de.meply.meply.data

import com.google.gson.annotations.SerializedName

/**
 * Request for POST /auth/register-with-code
 */
data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("registrationCode") val registrationCode: String
)

/**
 * Response from POST /auth/register-with-code
 */
data class RegisterResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: RegisteredUser?
)

data class RegisteredUser(
    @SerializedName("id") val id: Int?,
    @SerializedName("email") val email: String?,
    @SerializedName("confirmationToken") val confirmationToken: String?
)
