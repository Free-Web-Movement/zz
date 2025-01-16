package io.github.freewebmovement.zz.system.net.api.json

import kotlinx.serialization.Serializable

@Serializable
data class MessageSenderJSON(
    var sender: String,
    var message: String,
    var createdAt: Long,
)

@Serializable
data class MessageReceiverJSON(
    var receivedAt: Long,
    var code: Int = 0
)