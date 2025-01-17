package io.github.freewebmovement.zz.system.net.api.json

import kotlinx.serialization.Serializable

@Serializable
data class SignJSON(
    var json: String,
    var signature: String
)