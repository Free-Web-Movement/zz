package io.github.freewebmovement.peer.json

import io.github.freewebmovement.peer.IPType
import kotlinx.serialization.Serializable

@Serializable
data class PublicKeyJSON(
    var key: String? = null,
    var ip: String? = null,
    var port: Int? = null,
    var type: IPType? = null,
    var authCode: String? = null,
    var code : Int? = 0
    )
