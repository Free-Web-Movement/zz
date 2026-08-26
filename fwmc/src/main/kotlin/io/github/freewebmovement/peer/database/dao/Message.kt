package io.github.freewebmovement.peer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.freewebmovement.peer.database.entity.Message

@Dao
interface Message {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(message: Message)

    @Query("SELECT * FROM message ORDER BY created_at DESC")
    suspend fun getAll(): List<Message>

    @Query("SELECT * FROM message where `to` = :address and is_received = false ORDER BY created_at DESC")
    suspend fun getMessagesByAddress(address: String ): List<Message>

    @Query("UPDATE message set is_received = true where `to` = :address")
    suspend fun updateMessagesByAddress(address: String )

    @Query("DELETE FROM message")
    suspend fun clearData()
    @Query("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'message'")
    suspend fun clearSequence()

    @Update
    suspend fun update(message: Message)
    @Delete
    suspend fun delete(message: Message)
}