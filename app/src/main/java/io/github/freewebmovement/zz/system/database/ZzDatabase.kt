package io.github.freewebmovement.zz.system.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.database.entity.Peer

@Database(
    entities = [Peer::class, Message::class, Account::class],
    version = 1,
    exportSchema = false
)
abstract class ZzDatabase : RoomDatabase() {

    abstract fun peer(): io.github.freewebmovement.peer.database.dao.Peer
    abstract fun message(): io.github.freewebmovement.peer.database.dao.Message
    abstract fun account(): io.github.freewebmovement.peer.database.dao.Account

    companion object {

        @Volatile
        private var instance: ZzDatabase? = null

        fun getDatabase(context: Context): ZzDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            if (instance == null) {
                synchronized(this) {
                    // Pass the database to the INSTANCE
                    instance = buildDatabase(context)
                }
            }
            // Return database.
            return instance!!
        }

        private fun buildDatabase(context: Context): ZzDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ZzDatabase::class.java,
                "zz_database"
            )
                .build()
        }
    }
}