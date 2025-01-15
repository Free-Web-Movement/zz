package io.github.freewebmovement.zz.system.net.api.json

import android.provider.ContactsContract.CommonDataKinds.Nickname
import io.github.freewebmovement.zz.system.database.entity.AddressType
import kotlinx.serialization.Serializable

@Serializable
data class UserJSON(
    var nickname: String = "",
    var signature: String = "",
    var avatar: String = ""
)