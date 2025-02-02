package io.github.freewebmovement.peer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.freewebmovement.peer.types.IPType
import io.github.freewebmovement.peer.system.Time
import java.util.UUID.randomUUID


@Entity(tableName = "peer")
data class Peer(
    @ColumnInfo(name = "ip")
    var ip: String,
    @ColumnInfo(name = "port")
    var port: Int,
    @ColumnInfo(name = "ip_type")
    var ipType: IPType,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "accessible")
    var accessible: Boolean = false

    @ColumnInfo(name = "account")
    var account: Int = 0

    @ColumnInfo(name = "note")
    var note: String = ""

    @ColumnInfo(name = "created_at")
    var createdAt: Long = Time.now()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = Time.now()

    @ColumnInfo(name = "latest_seen")
    var latestSeen: Long = Time.now()

    var isTesting: Boolean = false

    val baseUrl: String
        get() {
            if (isTesting) return ""
            return when (ipType) {
                IPType.IPV6 -> "http://[$ip]:$port"
                else -> "http://$ip:$port"
            }
        }
    val getCode: String
        get() {
            return randomUUID().toString()
        }
}