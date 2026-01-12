package de.meply.meply.data.messages

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /conversations/me
 */
data class ConversationsResponse(
    @SerializedName("data") val data: List<Conversation>
)

data class Conversation(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String,
    @SerializedName("participants") val participants: List<Participant>,
    @SerializedName("hasUnread") val hasUnread: Boolean = false,
    @SerializedName("lastMessageAt") val lastMessageAt: String?,
    @SerializedName("meeting") val meeting: MeetingReference? = null
)

data class Participant(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String,
    @SerializedName("username") val username: String,
    @SerializedName("userslug") val userslug: String?,
    @SerializedName("avatar") val avatar: List<AvatarItem>? = null
)

data class AvatarItem(
    @SerializedName("url") val url: String
)

data class MeetingReference(
    @SerializedName("documentId") val documentId: String,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("event") val event: EventReference?,
    @SerializedName("location") val location: LocationReference?
)

data class EventReference(
    @SerializedName("Title") val title: String?
)

data class LocationReference(
    @SerializedName("Titel") val titel: String?
)

/**
 * Response from GET /conversations/{id}/messages
 */
data class MessagesResponse(
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("conversationClosed") val conversationClosed: Boolean = false
)

data class Message(
    @SerializedName("id") val id: Int,
    @SerializedName("documentId") val documentId: String,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("author") val author: MessageAuthor,
    @SerializedName("deletedByUser") val deletedByUser: Boolean = false,
    @SerializedName("conversationClosed") val conversationClosed: Boolean = false
)

data class MessageAuthor(
    @SerializedName("documentId") val documentId: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatar: List<AvatarItem>? = null
)

/**
 * Request for POST /messages (reply to existing conversation)
 */
data class SendMessageRequest(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("message") val message: String
)

/**
 * Request for POST /conversations/create (new conversation)
 */
data class CreateConversationRequest(
    @SerializedName("recipient") val recipient: String,
    @SerializedName("message") val message: String,
    @SerializedName("reference") val reference: String? = null
)

/**
 * Response from sending message
 */
data class SendMessageResponse(
    @SerializedName("data") val data: Message
)

/**
 * Request for DELETE /conversations/{id}
 */
data class DeleteConversationRequest(
    @SerializedName("convoId") val convoId: String
)
