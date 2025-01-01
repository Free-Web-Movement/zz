package io.github.freewebmovement.zz.system.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["session_id"], unique = true)], tableName = "peer")
data class Peer(
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    @PrimaryKey(autoGenerate=true) var id: Int = 0
    @ColumnInfo(name = "session_id") var sessionId: String = ""
//    @ColumnInfo(name = "nickname") var nickname: String = "Nick Name"
    @ColumnInfo(name = "rsa_public_key") var rsaPublicKey: String = ""
    @ColumnInfo(name = "ip_address") var ipAddress: String = ""
    @ColumnInfo(name = "is_ipv6") var isIpv6: Boolean = true
    @ColumnInfo(name = "ip_port") var ipPort: Int = 0
    @ColumnInfo(name = "note") var note: String = ""
}