package io.github.freewebmovement.zz.system.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "message")
data class Message(
    @ColumnInfo(name = "is_sending")
    var isSending: Boolean,
    @ColumnInfo(name = "is_succeeded")
    var isSucceeded: Boolean,
    @ColumnInfo(name = "from")
    var from: String,
    @ColumnInfo(name = "to")
    var to: String,
    @ColumnInfo(name = "message")
    var message: String,
    @ColumnInfo(name = "created_at")
    var createdAt: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    @ColumnInfo(name = "received_at")
    var receivedAt: Long = 0

}