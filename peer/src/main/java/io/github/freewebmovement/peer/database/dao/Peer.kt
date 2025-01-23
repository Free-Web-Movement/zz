package io.github.freewebmovement.peer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.freewebmovement.peer.database.entity.Peer

@Dao
interface Peer {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(peer: Peer)

    @Query("SELECT * FROM peer ORDER BY created_at DESC")
    suspend fun getAll(): List<Peer>

    @Query("SELECT * FROM peer where access_verification_code = :code")
    suspend fun getByCode(code: String): Peer

    @Query("DELETE FROM peer")
    suspend fun clearData()
    @Query("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'peer'")
    suspend fun clearSequence()

    @Update
    suspend fun update(peer: Peer)

    @Delete
    suspend fun delete(peer: Peer)
}