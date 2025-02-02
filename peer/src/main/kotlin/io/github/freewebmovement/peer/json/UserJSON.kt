package io.github.freewebmovement.peer.json

import kotlinx.serialization.Serializable

@Serializable
data class UserJSON(
    var nickname: String = "",
    var intro: String = "",
    var avatar: String = ""
    )