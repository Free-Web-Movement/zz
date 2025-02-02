package io.github.freewebmovement.peer.interfaces

import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.database.entity.Peer
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.types.IPType
import java.io.File

interface IInstrumentedHandler {
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun initAccount(publicKeyJSON: PublicKeyJSON)
    suspend fun getAccountByAddress(address: String): Account?


    suspend fun addPeer(peer: Peer)
    suspend fun updatePeer(peer: Peer)
    suspend fun initPeer(ip: String, port: Int, ipType: IPType)

    suspend fun getMessagesByAddress(address: String): List<Message>
    suspend fun updateMessagesByAddress(address: String)
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)

    suspend fun getPublicKeyJSON(keyOnly: Boolean = false): PublicKeyJSON

    fun getDownloadDir(): File
    fun getProfile(): UserJSON
    fun getCrypto(): Crypto
}