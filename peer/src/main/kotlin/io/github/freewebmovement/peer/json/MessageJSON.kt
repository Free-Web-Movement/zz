package io.github.freewebmovement.peer.json
import kotlinx.serialization.Serializable

@Serializable
data class MessageSenderJSON(
    var sender: String,
    var message: String,
    var createdAt: Long
)

@Serializable
data class MessageReceiverJSON(
    var receivedAt: Long,
    var code: Int = 0
)

@Serializable
data class MessagePollingSenderJSON(
    var sender: String,
)

@Serializable
data class Message(
    val from: String,
    val to : String,
    var message: String,
    val createdAt: Long,
)

@Serializable
data class MessagePollingReceiverJSON(
    var messages: ArrayList<Message>,
    var code: Int = 0
)

