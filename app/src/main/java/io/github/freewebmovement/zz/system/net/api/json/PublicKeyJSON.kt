package io.github.freewebmovement.zz.system.net.api.json

import kotlinx.serialization.Serializable

@Serializable
data class PublicKeyJSON(
    var rsaPublicKey: String,
    var ip: String? = null,
    var port: Int? = null,
    var session: String? = null
)