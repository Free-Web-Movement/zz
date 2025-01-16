package io.github.freewebmovement.zz.system.net.api.json

import kotlinx.serialization.Serializable

@Serializable
data class UserJSON(
    var nickname: String = "",
    var signature: String = "",
    var avatar: String = ""
)