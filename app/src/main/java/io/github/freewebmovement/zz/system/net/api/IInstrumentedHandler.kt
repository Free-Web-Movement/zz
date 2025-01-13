package io.github.freewebmovement.zz.system.net.api

import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.ktor.util.hex
import java.io.File
import java.security.PublicKey

interface IInstrumentedHandler {
    suspend fun addPeer(peer: Peer)
    suspend fun updatePeer(peer: Peer)
    suspend fun getPeerBySessionId(id: String): Peer
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun getPublicKeyJSON(publicKey: PublicKey): PublicKeyJSON

    fun getDownloadDir(): File
    fun getCrypto(): Crypto
}

class RoomHandler(var app: MainApplication) : IInstrumentedHandler {
    override suspend fun addPeer(peer: Peer) {
        app.db.peer().add(peer)
    }

    override suspend fun updatePeer(peer: Peer) {
        app.db.peer().update(peer)
    }

    override suspend fun getPeerBySessionId(id: String): Peer {
        return app.db.peer().getBySessionId(id)
    }

    override suspend fun addMessage(message: Message) {
        app.db.message().update(message)

    }

    override suspend fun updateMessage(message: Message) {
        app.db.message().update(message)
    }

    override suspend fun getPublicKeyJSON(publicKey: PublicKey): PublicKeyJSON {
        val json = PublicKeyJSON(hex(publicKey.encoded))
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

}