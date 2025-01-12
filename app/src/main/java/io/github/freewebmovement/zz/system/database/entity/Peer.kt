package io.github.freewebmovement.zz.system.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.freewebmovement.zz.system.Time

enum class AddressType {
    IPV4,
    IPV6,
    DOMAIN
}

@Entity(indices = [Index(value = ["session_id"], unique = true)], tableName = "peer")
data class Peer(
    @ColumnInfo(name = "address")
    var address: String,
    @ColumnInfo(name = "port")
    var port: Int,
    @ColumnInfo(name = "address_type")
    var addressType: AddressType,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "session_id")
    var sessionId: String = ""

    @ColumnInfo(name = "peer_session_id")
    var peerSessionId: String = ""

    @ColumnInfo(name = "nickname")
    var nickname: String = "Nick Name"

    @ColumnInfo(name = "signature")
    var signature: String = "Signature"

    @ColumnInfo(name = "avatar")
    var avatar: String = "avatar"

    @ColumnInfo(name = "rsa_public_key")
    var rsaPublicKeyByteArray: String = ""

    @ColumnInfo(name = "note")
    var note: String = ""

    @ColumnInfo(name = "latest_seen")
    var latestSeen: Long = Time.now()

    val baseUrl: String
        get() {
            return when (addressType) {
                AddressType.IPV6 -> "http://[$address]:$port"
                else -> "http://$address:$port"
            }
        }
}