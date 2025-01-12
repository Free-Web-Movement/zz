package io.github.freewebmovement.zz.system.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.database.entity.PeerMessages

@Dao
interface Peer {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(peer: Peer)

    @Query("SELECT * FROM peer ORDER BY created_at DESC")
    fun getAll(): List<Peer>

    @Query("SELECT * FROM peer where session_id = :sessionId")
    fun getBySessionId(sessionId: String): Peer

    @Transaction
    @Query("SELECT * FROM peer where id = :peer")
    fun getMessage(peer: Long): List<PeerMessages>

    @Query("DELETE FROM peer")
    fun clearData()
    @Query("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'peer'")
    fun clearSequence()

    @Update
    suspend fun update(peer: Peer)

    @Delete
    suspend fun delete(peer: Peer)
}