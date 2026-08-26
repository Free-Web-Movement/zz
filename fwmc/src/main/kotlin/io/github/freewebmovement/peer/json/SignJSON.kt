package io.github.freewebmovement.peer.json

import kotlinx.serialization.Serializable

@Serializable
data class SignJSON(
    var json: String,
    var signature: String,
    var publicKey: String
)