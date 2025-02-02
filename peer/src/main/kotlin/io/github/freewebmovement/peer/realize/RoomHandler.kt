package io.github.freewebmovement.peer.realize

import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.database.entity.Peer
import io.github.freewebmovement.peer.interfaces.IApp
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.interfaces.IInstrumentedHandler
import io.github.freewebmovement.peer.types.IPType
import io.ktor.util.hex
import kotlinx.coroutines.launch
import java.io.File


class RoomHandler(private var app: IApp) : IInstrumentedHandler {
    override suspend fun addAccount(account: Account) {
        var find = app.db.account().getAccountByAddress(account.address)
        if(find != null ) {
            account.id = find.id
            return
        }
        app.db.account().add(account)
        find = app.db.account().getAccountByAddress(account.address)
        assert(find != null)
        account.id = find!!.id
    }

    override suspend fun updateAccount(account: Account) {
        app.db.account().update(account)
    }

    override suspend fun initAccount(publicKeyJSON: PublicKeyJSON) {
        val publicKey = Crypto.fromHexString(publicKeyJSON.key!!)
        val address = Crypto.toAddress(publicKey)
        var account = Account(address)
        account.publicKey = publicKeyJSON.key!!
        addAccount(account)
        account = getAccountByAddress(address)!!
        val peer = Peer(
            ip = publicKeyJSON.ip!!,
            port = publicKeyJSON.port!!,
            ipType = publicKeyJSON.type!!,
        )
        peer.account = account.id
        addPeer(peer)
    }

    override suspend fun addPeer(peer: Peer) {
        app.db.peer().add(peer)
    }

    override suspend fun updatePeer(peer: Peer) {
        app.db.peer().update(peer)
    }

    override suspend fun initPeer(ip: String, port: Int, ipType: IPType) {
        val scope = app.scope

        scope.launch {
            // 1. Get Public Key From the Peer
            val peer = Peer(ip = ip, port = port, ipType = ipType)
            app.client.getPublicKey(peer)
            // 2. Tell My Info to Peer
            app.client.setPublicKey(peer, getCrypto())

            // 3. Receive New Request From Peer to Verify My Accessibility
        }

    }

    override suspend fun getMessagesByAddress(address: String): List<Message> {
        return app.db.message().getMessagesByAddress(address)
    }

    override suspend fun updateMessagesByAddress(address: String) {
        app.db.message().updateMessagesByAddress(address)
    }

    override suspend fun getAccountByAddress(address: String): Account? {
        return app.db.account().getAccountByAddress(address)
    }

    override suspend fun addMessage(message: Message) {
        app.db.message().add(message)
    }

    override suspend fun updateMessage(message: Message) {
        app.db.message().update(message)
    }

    override suspend fun getPublicKeyJSON(keyOnly: Boolean): PublicKeyJSON {
        val json = PublicKeyJSON(hex(app.crypto.publicKey.encoded))
        if(keyOnly) return json
        app.setIpInfo(json)
        return json
    }

    override fun getDownloadDir(): File {
        return app.getDownloadDir()
    }

    override fun getProfile(): UserJSON {
        return app.getProfile()
    }

    override fun getCrypto(): Crypto {
        return app.crypto
    }
}