package de.meply.meply.data.onboarding

import com.google.gson.annotations.SerializedName

data class CompleteOnboardingResponse(
    @SerializedName("data") val data: OnboardingData?
)

data class OnboardingData(
    @SerializedName("isOnboarded") val isOnboarded: String?
)
