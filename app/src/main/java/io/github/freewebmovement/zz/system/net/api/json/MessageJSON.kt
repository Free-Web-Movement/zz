package io.github.freewebmovement.zz.system.net.api.json

import kotlinx.serialization.Serializable

@Serializable
data class MessageSenderJSON(
    var message: String,
    var createdAt: Long,
    var sessionId: String? = null
)

@Serializable
data class MessageReceiverJSON(
    var receivedAt: Long,
)