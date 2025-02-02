package io.github.freewebmovement.peer

import io.github.freewebmovement.peer.database.entity.Account
import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.database.entity.Peer
import io.github.freewebmovement.peer.json.MessagePollingReceiverJSON
import io.github.freewebmovement.peer.json.MessagePollingSenderJSON
import io.github.freewebmovement.peer.json.MessageReceiverJSON
import io.github.freewebmovement.peer.json.MessageSenderJSON
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.system.Time
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.interfaces.IInstrumentedHandler
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.util.cio.writeChannel
import io.ktor.util.hex
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.Json
import java.io.File
import java.security.PublicKey

class PeerClient(private var execute: IInstrumentedHandler) {
    val client = HttpClient(CIO)
    // Step 1. Initial step to get public key from a peer server
    suspend fun getPublicKey(peer: Peer): PublicKey {
        val response = client.get(peer.baseUrl + "/api/key/public")
        val jsonStr = response.bodyAsText()
        val json = Crypto.verify<PublicKeyJSON>(jsonStr)
        val publicKey = Crypto.fromHexString(json.key!!)
        val address = Crypto.toAddress(publicKey)
        val account = Account(address = address)
        account.publicKey = hex(publicKey.encoded)
        execute.addAccount(account)
        peer.account = account.id
        peer.accessible = true
        execute.addPeer(peer)
        return publicKey
    }

    // Step 2. send your public key to the server
    suspend fun setPublicKey(peer: Peer, crypto: Crypto): HttpResponse {
        val json = execute.getPublicKeyJSON()
        execute.updatePeer(peer)
        val response = client.post(peer.baseUrl + "/api/key/public") {
            setBody(Crypto.sign(json, crypto))
        }
        val resStr = response.body<String>()
        val resJson = Crypto.verify<PublicKeyJSON>(resStr)
        assert(resJson.code == 0)
        peer.latestSeen = Time.now()
        execute.updatePeer(peer)
        return response
    }


    // Step 3. Now can start messaging from peer to peer
    suspend fun sendMessage(
        sendStr: String,
        peer: Peer,
        from: Crypto,
        to: Account
    ): HttpResponse {
        // Sending your address as session id
        val messageJSON = MessageSenderJSON(
            Crypto.toAddress(from.publicKey), "", Time.now()
        )
        messageJSON.message = Crypto.encrypt(sendStr, Crypto.fromHexString(to.publicKey))
        val message = Message(
            isSending = true,
            isSucceeded = false,
            from = Crypto.toAddress(from
                .publicKey),
            to = to.address,
            message = sendStr,
            createdAt = messageJSON.createdAt
        )
        execute.addMessage(message)
//        val json = Json.encodeToString(messageJSON)
        val response = client.post(peer.baseUrl + "/api/message") {
            setBody(Crypto.sign(messageJSON, from))
        }
        val receiverStr = response.body<String>()
        val messageReceiverJSON = Crypto.verify<MessageReceiverJSON>(receiverStr)
        message.isSucceeded = true
        message.receivedAt = messageReceiverJSON.receivedAt
        execute.updateMessage(message)
        return response
    }

    // Step 4. Pull Message from peers
    suspend fun pullMessage(
        peer: Peer,
        from: Crypto,
    ): HttpResponse {
        val messagePollingSenderJSON = MessagePollingSenderJSON(
            Crypto.toHexString(from.publicKey)
        )
        val response = client.get(peer.baseUrl + "/api/message") {
            setBody(Crypto.sign(messagePollingSenderJSON, from))
        }
        val receiverStr = response.body<String>()
        val messagePollingReceiverJSON = Crypto.verify<MessagePollingReceiverJSON>(receiverStr)
        messagePollingReceiverJSON.messages.forEach {
            val str = Crypto.decrypt(it.message, from.privateKey)
            it.message = str
            val message = Message(
                from = it.from,
                message = it.message,
                to = it.to,
                createdAt = it.createdAt,
                isSending = false,
                isSucceeded = true
            )
            execute.addMessage(message)
        }
        return response
    }

    suspend fun getProfile(peer: Peer, account: Account): HttpResponse {
        val response = client.get(peer.baseUrl + "/user/profile")
        val jsonStr = response.body<String>()
        val user = Json.decodeFromString<UserJSON>(jsonStr)
        account.avatar = user.avatar
        account.nickname = user.nickname
        account.intro = user.intro
        execute.updateAccount(account)
        return response
    }

    /**
     * get apk file from server, for test only
     */
    suspend fun getApkFile(peer: Peer): HttpResponse {
        val apkFile = File(execute.getDownloadDir().path + "/base.apk")
        val response = client.get(peer.baseUrl + "/download/apk")
        response.bodyAsChannel().copyAndClose(
            apkFile.writeChannel()
        )
        return response
    }
}