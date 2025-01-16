package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.api.json.MessageReceiverJSON
import io.github.freewebmovement.zz.system.net.api.json.MessageSenderJSON
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.github.freewebmovement.zz.system.net.api.json.UserJSON
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.internal.wait

class PeerClient(private var client: HttpClient, private var execute: IInstrumentedHandler) {
    // Step 1. Initial step to get public key from a peer server
    suspend fun getPublicKey(peer: Peer): HttpResponse {

        val response = client.get(peer.baseUrl + "/api/key/public")
        val jsonStr = response.bodyAsText()
        val json = Json.decodeFromString<PublicKeyJSON>(jsonStr)
//        account.publicKey = json.key!!

        // Save Peer and Account address
        val publicKey = Crypto.toPublicKey(json.key!!)
        val address = Crypto.toAddress(publicKey)
        val account = Account(address = address)
        account.publicKey = json.key!!
        execute.addAccount(account)
        execute.addPeer(peer)
        return response
    }

    // Step 2. send your public key to the server
    suspend fun setPublicKey(peer: Peer): HttpResponse {
        val json = execute.getPublicKeyJSON()
        val response = client.post(peer.baseUrl + "/api/key/public") {
            setBody(Json.encodeToString(json))
        }
        val resStr = response.body<String>()
        val resJson = Json.decodeFromString<PublicKeyJSON>(resStr)
        assert(resJson.code == 0)
        peer.latestSeen = Time.now()
        execute.updatePeer(peer)
        return response
    }

    // Step 3. Now You can start messaging
    suspend fun sendMessage(sendStr: String, peer: Peer, from: Account, to: Account): HttpResponse {
        // Sending your address as session id
        val messageJSON = MessageSenderJSON(from.address, "", Time.now())
        messageJSON.message = execute.encrypt(sendStr, to)
        val message = Message(
            isSending = true,
            isSucceeded = false,
            from = from.address,
            to = to.address,
            message = sendStr,
            createdAt = messageJSON.createdAt
        )
        execute.addMessage(message)
        val response = client.post(peer.baseUrl + "/api/message") {
            setBody(Json.encodeToString(messageJSON))
        }
        val receiverStr = response.body<String>()

        val messageReceiverJSON = Json.decodeFromString<MessageReceiverJSON>(receiverStr)
        message.isSucceeded = true
        message.receivedAt = messageReceiverJSON.receivedAt
        execute.updateMessage(message)
        return response
    }

    suspend fun getProfile(peer: Peer, account: Account) : HttpResponse {
        val response = client.get(peer.baseUrl + "/user/profile")
        val jsonStr = response.body<String>()
        val user = Json.decodeFromString<UserJSON>(jsonStr)
        account.avatar = user.avatar
        account.nickname = user.nickname
        account.signature = user.signature
        execute.updateAccount(account)
        return response
    }

    /**
     * get apk file from server, for test only
     */
    suspend fun getApkFile(peer: Peer): HttpResponse {
        var response: HttpResponse? = null
        val temp = client.prepareRequest {
            url(peer.baseUrl + "/download/apk")
        }
        temp.execute { r ->
            r.bodyAsChannel().copyAndClose(
                execute.getDownloadDir().writeChannel()
//                File(
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//                    app.applicationContext.packageName
//                ).writeChannel()
            )
            response = r
        }.wait()
        return response!!
    }
}