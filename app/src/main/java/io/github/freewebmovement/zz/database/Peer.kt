package io.github.freewebmovement.zz.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["session_id"], unique = true)])
data class Peer(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "rsa_public_key") val rsaPublicKey: String?,
    @ColumnInfo(name = "ip_address") val ipAddress: String?,
    @ColumnInfo(name = "ip_port") val ipPort: Int?,
    @ColumnInfo(name = "note") val note: String?
)