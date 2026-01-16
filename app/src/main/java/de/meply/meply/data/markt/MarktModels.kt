package de.meply.meply.data.markt

import com.google.gson.annotations.SerializedName

/**
 * Response from /user-boardgames/marketplace endpoint
 */
data class MarktplaceResponse(
    @SerializedName("results") val results: List<MarktGame>?,
    @SerializedName("count") val count: Int?,
    @SerializedName("pagination") val pagination: MarktPagination?
)

/**
 * Pagination info for marketplace
 */
data class MarktPagination(
    @SerializedName("page") val page: Int?,
    @SerializedName("pageSize") val pageSize: Int?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("total") val total: Int?
)

/**
 * A game with its offers in the marketplace
 */
data class MarktGame(
    @SerializedName("title") val title: String?,
    @SerializedName("offers") val offers: List<MarktOffer>?
)

/**
 * An individual offer for a game
 */
data class MarktOffer(
    @SerializedName("user") val user: MarktUser?,
    @SerializedName("userSlug") val userSlug: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("condition") val condition: String?,
    @SerializedName("delivery") val delivery: String?,
    @SerializedName("tradePossible") val tradePossible: Boolean?,
    @SerializedName("description") val description: String?
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
    fun getLocalizedDelivery(): String {
        return when (delivery) {
            "ShippingOnly" -> "Nur Versand"
            "PickupOnly" -> "Nur Abholung"
            "ShippingOrPickup" -> "Versand oder Abholung"
            else -> delivery ?: ""
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

/**
 * User info in a marketplace offer
 */
data class MarktUser(
    @SerializedName("username") val username: String?,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("city") val city: String?
)
