package de.meply.meply.data.profile

import com.google.gson.annotations.SerializedName

/**
 * Response from POST /registration-codes/my-invite-codes
 */
data class InviteCodesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: InviteCodesData
)

data class InviteCodesData(
    @SerializedName("codes") val codes: List<InviteCode>,
    @SerializedName("stats") val stats: InviteCodeStats
)

data class InviteCode(
    @SerializedName("code") val code: String,
    @SerializedName("used") val used: Boolean = false,
    @SerializedName("usedAt") val usedAt: String? = null
)

data class InviteCodeStats(
    @SerializedName("totalCodes") val totalCodes: Int,
    @SerializedName("unusedCodes") val unusedCodes: Int,
    @SerializedName("newCodesGenerated") val newCodesGenerated: Int = 0,
    @SerializedName("canGenerateMore") val canGenerateMore: Int = 0,
    @SerializedName("limits") val limits: InviteCodeLimits? = null
)

data class InviteCodeLimits(
    @SerializedName("maxUnused") val maxUnused: Int,
    @SerializedName("maxTotal") val maxTotal: Int
)
