package io.github.freewebmovement.zz.system.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.freewebmovement.zz.system.database.entity.Message

@Dao
interface Message {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(message: Message)

    @Query("SELECT * FROM message ORDER BY created_at DESC")
    fun getAll(): List<Message>

    @Query("DELETE FROM message")
    fun clearData()
    @Query("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'message'")
    fun clearSequence()

    @Update
    suspend fun update(message: Message)
    @Delete
    suspend fun delete(message: Message)
}