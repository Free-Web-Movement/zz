package io.github.freewebmovement.zz.system.net.api

import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.ktor.util.hex
import java.io.File

interface IInstrumentedHandler {
    suspend fun addPeer(peer: Peer)
    suspend fun updatePeer(peer: Peer)
    suspend fun getPeerBySessionId(sessionId: String): Peer
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun getPublicKeyJSON(keyOnly: Boolean = false): PublicKeyJSON
    fun getDownloadDir(): File
    fun getCrypto(): Crypto
    fun decrypt(enc:String): String
    fun encrypt(dec:String, peer: Peer): String
}

class RoomHandler(var app: MainApplication) : IInstrumentedHandler {
    override suspend fun addPeer(peer: Peer) {
        app.db.peer().add(peer)
    }

    override suspend fun updatePeer(peer: Peer) {
        app.db.peer().update(peer)
    }

    override suspend fun getPeerBySessionId(sessionId: String): Peer {
        return app.db.peer().getBySessionId(sessionId)
    }

    override suspend fun addMessage(message: Message) {
        app.db.message().update(message)
    }

    override suspend fun updateMessage(message: Message) {
        app.db.message().update(message)
    }

    override suspend fun getPublicKeyJSON(isKeyOnly: Boolean): PublicKeyJSON {
        val json = PublicKeyJSON(hex(app.crypto.publicKey.encoded))
        if(isKeyOnly) return json
        json.ip = app.ipList.getUri()
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

    override fun getCrypto(): Crypto {
        return app.crypto
    }

    override fun decrypt(enc: String): String {
        return Crypto.decrypt(enc, app.crypto.privateKey)
    }

    override fun encrypt(dec: String, peer: Peer): String {
        val rsaPublicKey = Crypto.revokePublicKey(peer.rsaPublicKeyByteArray.toByteArray())
        return Crypto.encrypt(dec, rsaPublicKey)
    }
}