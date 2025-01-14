package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.api.json.MessageReceiverJSON
import io.github.freewebmovement.zz.system.net.api.json.MessageSenderJSON
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
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
import io.ktor.server.sessions.generateSessionId
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.internal.wait

class PeerClient(private var client : HttpClient, private var execute: IInstrumentedHandler) {
    // Step 1. Initial step to get public key from a peer server
    suspend fun getPublicKey(peer: Peer): HttpResponse {
        val response = client.get(peer.baseUrl + "/api/key/public")
        val jsonStr = response.bodyAsText()
        val json = Json.decodeFromString<PublicKeyJSON>(jsonStr)
        peer.rsaPublicKeyByteArray = json.rsaPublicKeyByteArray!!
        execute.addPeer(peer)
        return response
    }

    // Step 2. send your public key to the server
    suspend fun setPublicKey(peer: Peer): HttpResponse {
        val sessionId = generateSessionId()
        val json = execute.getPublicKeyJSON()
        json.sessionId = sessionId
        peer.sessionId = sessionId
        val response = client.post(peer.baseUrl + "/api/key/public") {
            setBody(Json.encodeToString(json))
        }
        val resStr = response.body<String>()
        val decodedJSON = Json.decodeFromString<PublicKeyJSON>(resStr)
        peer.latestSeen = Time.now()
        peer.peerSessionId = decodedJSON.sessionId.toString()
        execute.updatePeer(peer)
        return response
    }

    // Step 3. Now You can start messaging
    suspend fun sendMessage(messageStr: String, peer: Peer): HttpResponse {
        //
        val messageJSON = MessageSenderJSON(messageStr, Time.now(), peer.sessionId)
        val sendStr = execute.encrypt(Json.encodeToString(messageJSON), peer)
//        val sendStr = Crypto.encrypt(Json.encodeToString(messageJSON), rsaPublicKey)
        val message = Message(
            isSending = true,
            isSucceeded = false,
            peer = peer.id,
            message = messageStr,
            createdAt = messageJSON.createdAt
        )
        execute.addMessage(message)
        //app.db.message().add(message)
        val response = client.post(peer.baseUrl + "/api/message") {
            setBody(sendStr)
        }
        val encodeStr = response.body<String>()

        val decodedStr = execute.decrypt(encodeStr)
//        val decodedStr = Crypto.decrypt(encodeStr, crypto.privateKey)
        val messageReceiverJSON = Json.decodeFromString<MessageReceiverJSON>(decodedStr)
        if(response.status.value == 200) {
            message.isSucceeded = true
            message.receivedAt = messageReceiverJSON.receivedAt
            execute.updateMessage(message)
//            app.db.message().update(message)
            return response
        } else {
            throw Exception(response.status.description)
        }
    }

        /**
     * get apk file from server, for test only
     */
    suspend fun getApkFile(peer: Peer) :  HttpResponse {
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