package de.meply.meply.data.profile

import com.google.gson.annotations.SerializedName

/**
 * Response from /user-boardgames/sales/{profileId} endpoint
 */
data class UserSalesResponse(
    @SerializedName("sales") val sales: List<UserSaleItem>?
)

/**
 * Individual sale item from user's flea market listings
 */
data class UserSaleItem(
    @SerializedName("title") val title: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("condition") val condition: String?, // "New", "Like New", "Very Good", "Good", "Used"
    @SerializedName("deliveryOption") val deliveryOption: String?, // "ShippingOnly", "PickupOnly", "ShippingOrPickup"
    @SerializedName("tradePossible") val tradePossible: Boolean?
) {
    /**
     * Get localized condition string
     */
    fun getLocalizedCondition(): String {
        return when (condition) {
            "New" -> "Neu"
            "Like New" -> "Wie neu"
            "Very Good" -> "Sehr gut"
            "Good" -> "Gut"
            "Used" -> "Gebraucht"
            else -> condition ?: ""
        }
    }

    /**
     * Get localized delivery option string
     */
    fun getLocalizedDeliveryOption(): String {
        return when (deliveryOption) {
            "ShippingOnly" -> "Nur Versand"
            "PickupOnly" -> "Nur Abholung"
            "ShippingOrPickup" -> "Versand oder Abholung"
            else -> deliveryOption ?: ""
        }
    }

    /**
     * Get formatted price string
     */
    fun getFormattedPrice(): String {
        return if (price != null && price > 0) {
            String.format("%.2f €", price).replace(".", ",")
        } else {
            "–"
        }
    }
}
