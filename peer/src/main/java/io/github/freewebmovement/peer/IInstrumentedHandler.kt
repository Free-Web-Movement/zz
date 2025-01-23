package io.github.freewebmovement.peer

import io.github.freewebmovement.peer.system.crypto.Crypto

import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.SignJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.database.entity.Peer

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.PublicKey

interface IInstrumentedHandler {
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    // peer related operations
    fun initPeer(ip:String, port:Int, ipType: IPType)
    suspend fun addPeer(peer: Peer)
    suspend fun updatePeer(peer: Peer)
    suspend fun getPeerByCode(code: String): Peer
    suspend fun getAccountByAddress(address: String): Account?
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun getPublicKeyJSON(keyOnly: Boolean = false): PublicKeyJSON
    fun getDownloadDir(): File
    fun getProfile(): UserJSON
    fun getCrypto(): Crypto
    fun decrypt(enc:String): String
    fun encrypt(dec:String, account: Account? = null): String
    fun sign(message: String): ByteArray
    fun verify(message: String, signature: ByteArray, publicKey: PublicKey): Boolean
    fun accessVerify(code: String, peer: Peer, to: Account)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> IInstrumentedHandler.signType(json: T): String {
    val jsonStr = Json.encodeToString(json)
    val sign = sign(jsonStr).toHexString()
    return Json.encodeToString(SignJSON(jsonStr, sign))
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> IInstrumentedHandler.verifyType(json: String, publicKey: PublicKey): T {
    val signJSON = Json.decodeFromString<SignJSON>(json)
    assert(verify(signJSON.json, signJSON.signature.hexToByteArray(),
        publicKey))
    val resJson = Json.decodeFromString<T>(signJSON.json)
    return resJson
}
