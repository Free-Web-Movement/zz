package io.github.freewebmovement.peer.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.database.entity.Peer

@Database(
    entities = [Peer::class, Message::class, Account::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun peer(): io.github.freewebmovement.peer.database.dao.Peer
    abstract fun message(): io.github.freewebmovement.peer.database.dao.Message
    abstract fun account(): io.github.freewebmovement.peer.database.dao.Account
}
