package io.github.freewebmovement.zz.system.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.AccountPeer
import kotlinx.coroutines.flow.Flow

@Dao
interface Account {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(account: Account)

    @Query("SELECT * FROM account where id = :id")
    fun get(id: Int): Account?

    @Query("SELECT * FROM account ORDER BY created_at DESC")
    fun getAll(): List<Account>

    @Query("SELECT * FROM account ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<Account>>

    @Transaction
    @Query("SELECT * FROM account ORDER BY created_at DESC")
    fun getPeers(): List<AccountPeer>

    @Query("SELECT * FROM account where address = :address")
    fun getAccountByAddress(address: String): Account?
    
    @Query("DELETE FROM account")
    fun clearData()
    @Query("UPDATE sqlite_sequence SET seq = 0 WHERE name = 'account'")
    fun clearSequence()

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)
}