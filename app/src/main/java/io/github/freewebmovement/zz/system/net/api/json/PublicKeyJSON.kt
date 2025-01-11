package io.github.freewebmovement.zz.system.net.api.json

import io.github.freewebmovement.zz.system.database.entity.AddressType
import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class PublicKeyJSON(
    var rsaPublicKey: String? = null,
    var ip: String? = null,
    var port: Int? = null,
    var type: AddressType? = null,
    var sessionId: String? = null
)