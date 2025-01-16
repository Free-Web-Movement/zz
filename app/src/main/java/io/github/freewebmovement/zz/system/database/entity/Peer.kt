package io.github.freewebmovement.zz.system.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.freewebmovement.zz.system.Time


// Only IP is Allowed
enum class IPType {
    IPV4,
    IPV6
}

@Entity(tableName = "peer")
data class Peer(
    @ColumnInfo(name = "account")
    var account: Int,
    @ColumnInfo(name = "ip")
    var ip: String,
    @ColumnInfo(name = "port")
    var port: Int,
    @ColumnInfo(name = "ip_type")
    var ipType: IPType,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "note")
    var note: String = ""

    @ColumnInfo(name = "latest_seen")
    var latestSeen: Long = Time.now()

    var isTesting: Boolean = false

    val baseUrl: String
        get() {
            if(isTesting) return ""
            return when (ipType) {
                IPType.IPV6 -> "http://[$ip]:$port"
                else -> "http://$ip:$port"
            }
        }
}