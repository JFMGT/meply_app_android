package de.meply.meply.data.collection

import com.google.gson.annotations.SerializedName

/**
 * Response from /user-boardgames/my-collection
 */
data class MyCollectionResponse(
    @SerializedName("results") val results: List<UserBoardgame>?,
    @SerializedName("pagination") val pagination: CollectionPagination?
)

data class CollectionPagination(
    @SerializedName("page") val page: Int?,
    @SerializedName("pageSize") val pageSize: Int?,
    @SerializedName("pageCount") val pageCount: Int?,
    @SerializedName("total") val total: Int?
)

/**
 * A boardgame in the user's collection
 */
data class UserBoardgame(
    @SerializedName("id") val id: String,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName("state") val state: String?, // none, wishlist, played, owned
    @SerializedName("forSale") val forSale: Boolean?,
    @SerializedName("price") val price: Double?,
    @SerializedName("condition") val condition: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("deliveryOption") val deliveryOption: String?,
    @SerializedName("tradePossible") val tradePossible: Boolean?
) {
    fun getLocalizedState(): String {
        return when (state) {
            "wishlist" -> "Will spielen"
            "played" -> "Habe gespielt"
            "owned" -> "Besitze"
            else -> "Keine Angabe"
        }
    }
}

/**
 * Response from boardgame search
 */
data class BoardgameSearchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?
)

/**
 * Request to create a new boardgame
 */
data class CreateBoardgameRequest(
    @SerializedName("data") val data: CreateBoardgameData
)

data class CreateBoardgameData(
    @SerializedName("title") val title: String
)

/**
 * Response from creating a boardgame
 */
data class CreateBoardgameResponse(
    @SerializedName("data") val data: CreatedBoardgame?
)

data class CreatedBoardgame(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String?,
    @SerializedName("title") val title: String?
)

/**
 * Request to add a game to collection
 */
data class AddToCollectionRequest(
    @SerializedName("boardgame") val boardgameId: Int
)

/**
 * Response from adding game to collection
 */
data class AddToCollectionResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("id") val id: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("alreadyExists") val alreadyExists: Boolean?,
    @SerializedName("error") val error: String?
)

/**
 * Request to update a user boardgame entry
 */
data class UpdateUserBoardgameRequest(
    @SerializedName("rating") val rating: Float? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("forSale") val forSale: Boolean? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("condition") val condition: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("deliveryOption") val deliveryOption: String? = null,
    @SerializedName("tradePossible") val tradePossible: Boolean? = null
)

/**
 * Generic success response
 */
data class CollectionActionResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)
