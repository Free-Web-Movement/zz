package io.github.freewebmovement.zz.system.net.api.json

import io.github.freewebmovement.zz.system.database.entity.AddressType
import kotlinx.serialization.Serializable

@Serializable
data class PublicKeyJSON(
    var rsaPublicKeyByteArray: String? = null,
    var ip: String? = null,
    var port: Int? = null,
    var type: AddressType? = null,
    // enc
    var sessionId: String? = null
)