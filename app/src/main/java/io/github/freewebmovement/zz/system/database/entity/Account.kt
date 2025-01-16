package io.github.freewebmovement.zz.system.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.freewebmovement.zz.system.Time

@Entity(indices = [Index(value = ["address"], unique = true)], tableName = "account")
class Account(
    @ColumnInfo(name = "address")
    var address: String,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "public_key")
    var publicKey: String = ""

    @ColumnInfo(name = "nickname")
    var nickname: String = "Nick Name"

    @ColumnInfo(name = "signature")
    var signature: String = "Signature"

    @ColumnInfo(name = "avatar")
    var avatar: String = "avatar"

    @ColumnInfo(name = "created_at")
    var createdAt: Long = Time.now()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = Time.now()
}