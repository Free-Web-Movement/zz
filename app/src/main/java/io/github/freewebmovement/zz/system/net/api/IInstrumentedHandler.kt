package io.github.freewebmovement.zz.system.net.api

import android.net.Uri
import android.os.Environment
import io.github.freewebmovement.peer.IPType
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.Image
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.github.freewebmovement.zz.system.net.api.json.SignJSON
import io.github.freewebmovement.zz.system.net.api.json.UserJSON
import io.ktor.util.hex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

class RoomHandler(var app: MainApplication) : IInstrumentedHandler {
    override suspend fun addAccount(account: Account) {
        val find = app.db.account().getAccountByAddress(account.address)
        if(find != null ) {
            account.id = find.id
            return
        }
        app.db.account().add(account)
    }

    override suspend fun updateAccount(account: Account) {
        app.db.account().update(account)
    }

    override fun initPeer(ip: String, port: Int, ipType: IPType) {
        val scope = app.coroutineScope

        scope.launch {
            // 1. Get Public Key From the Peer
            val peer = Peer(ip = ip, port = port, ipType = ipType)
            val publicKey = app.peerClient.getPublicKey(peer)
            // 2. Tell My Info to Peer
            app.peerClient.setPublicKey(peer, publicKey)

            // 3. Receive New Request From Peer to Verify My Accessibility
        }

    }

    override suspend fun addPeer(peer: Peer) {
        app.db.peer().add(peer)
    }

    override suspend fun updatePeer(peer: Peer) {
        app.db.peer().update(peer)
    }

    override suspend fun getPeerByCode(code: String): Peer {
        return app.db.peer().getByCode(code)
    }

    override suspend fun getAccountByAddress(address: String): Account? {
        return app.db.account().getAccountByAddress(address)
    }

    override suspend fun addMessage(message: Message) {
        app.db.message().update(message)
    }

    override suspend fun updateMessage(message: Message) {
        app.db.message().update(message)
    }

    override suspend fun getPublicKeyJSON(keyOnly: Boolean): PublicKeyJSON {
        val json = PublicKeyJSON(hex(app.crypto.publicKey.encoded))
        if(keyOnly) return json
        json.ip = app.ipList.getPublicIP()
        json.port = app.settings.localServerPort
        json.type = app.ipList.getPublicType()
        return json
    }

    override fun getDownloadDir(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            app.applicationContext.packageName
        )
    }

    override fun getProfile(): UserJSON {
        val nickname = app.settings.mineProfileNickname
        val intro = app.settings.mineProfileIntro
        val imageUri = app.settings.mineProfileImageUri
        var avatar = ""
        if (imageUri != "") {
            val uri: Uri = Uri.parse(imageUri)
            avatar = Image.toBase64(app.applicationContext, uri)
        }
        return UserJSON(nickname = nickname, intro = intro, avatar = avatar)
    }

    override fun getCrypto(): Crypto {
        return app.crypto
    }

    override fun decrypt(enc: String): String {
        return Crypto.decrypt(enc, app.crypto.privateKey)
    }

    override fun encrypt(dec: String, account: Account?): String {
        if(account == null) {
            return Crypto.encrypt(dec, app.crypto.publicKey)
        }
        val rsaPublicKey = Crypto.toPublicKey(account.publicKey)
        return Crypto.encrypt(dec, rsaPublicKey)
    }

    override fun sign(message: String): ByteArray {
        return Crypto.sign(message.toByteArray(), app.crypto.privateKey)
    }

    override fun verify(message: String, signature: ByteArray, publicKey: PublicKey): Boolean {

        return Crypto.verify(message.toByteArray(), signature, publicKey)
    }

    override fun accessVerify(code: String, peer: Peer, to: Account) {
        val httpScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        val request = httpScope.launch {
            // suspend calls are allowed here cause this is a coroutine
            app.peerClient.verifyAccessibility(code, peer, Crypto.toPublicKey(to.publicKey))
        }
    }
}