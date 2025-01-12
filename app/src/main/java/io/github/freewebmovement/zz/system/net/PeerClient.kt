package io.github.freewebmovement.zz.system.net

import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.json.MessagReceiverJSON
import io.github.freewebmovement.zz.system.net.api.json.MessageSenderJSON
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.sessions.generateSessionId
import io.ktor.util.cio.writeChannel
import io.ktor.util.hex
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.internal.wait
import java.io.File

class PeerClient(var app: MainApplication, a: Peer) {
    val peer : Peer = a
    var client: HttpClient = HttpClient(CIO) {
    }

    // Step 1. Initial step to get public key from a peer server
    suspend fun getPublicKey(): HttpResponse {
        val response = client.get(peer.baseUrl + "/api/key/public")
        val json = response.body<PublicKeyJSON>()
        peer.rsaPublicKeyByteArray = json.rsaPublicKeyByteArray.toString()
        app.db.peer().add(peer)
        return response
    }

    // Step 2. send your public key to the server
    suspend fun setPublicKey(): HttpResponse {
        val sessionId = generateSessionId()
        val responseStr: String
        val json = PublicKeyJSON(hex(app.crypto.publicKey.encoded))
        if (app.ipList.hasPublicIPs()) {
            json.ip = app.ipList.getPublicUri()
            json.port = app.settings.localServerPort
            json.type = app.ipList.getPublicType()
            json.sessionId = sessionId
            peer.sessionId = sessionId
            app.db.peer().update(peer)
            val rsaPublicKey = Crypto.revokePublicKey(peer.rsaPublicKeyByteArray.toByteArray())
            responseStr = Crypto.encrypt(Json.encodeToString(json), rsaPublicKey)
        } else {
            throw Exception(app.applicationContext.getString(R.string.share_app_apk_no_public_ip))
        }


        val response = client.post(peer.baseUrl + "/api/key/public") {
            setBody(responseStr)
        }
        val resStr = response.body<String>()
        val decodedStr = Crypto.decrypt(resStr, app.crypto.privateKey)
        val decodedJSON = Json.decodeFromString<PublicKeyJSON>(decodedStr)
        decodedJSON.sessionId?.let { assert(it.isNotEmpty()) }
        peer.latestSeen = Time.now()
        peer.peerSessionId = decodedJSON.sessionId.toString()
        app.db.peer().update(peer)
        return response
    }

    // Step 3. Now You can start messaging
    suspend fun sendMessage(messageStr: String): HttpResponse {
        val rsaPublicKey = Crypto.revokePublicKey(peer.rsaPublicKeyByteArray.toByteArray())
//
        val messageJSON = MessageSenderJSON(messageStr, Time.now(), peer.sessionId)
        val sendStr = Crypto.encrypt(Json.encodeToString(messageJSON), rsaPublicKey)
        val message = Message(
            isSending = true,
            isSucceeded = false,
            peer = peer.id,
            message = messageStr,
            createdAt = messageJSON.createdAt
        )
        app.db.message().add(message)
        val response = client.post(peer.baseUrl + "/api/message") {
            setBody(sendStr)
        }
        val encodeStr = response.body<String>()

        val decodedStr = Crypto.decrypt(encodeStr, app.crypto.privateKey)
        val messageReceiverJSON = Json.decodeFromString<MessagReceiverJSON>(decodedStr)
        if(response.status.value == 200) {
            message.isSucceeded = true
            message.receivedAt = messageReceiverJSON.receivedAt
            app.db.message().update(message)
            return response
        } else {
            throw Exception(response.status.description)
        }
    }

        /**
     * get apk file from server, for test only
     */
    suspend fun getApkFile() :  HttpResponse {
        var response: HttpResponse? = null
        val temp = client.prepareRequest {
            url(peer.baseUrl + "/app/download/apk")
        }
        temp.execute { r ->
            r.bodyAsChannel().copyAndClose(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    app.applicationContext.packageName
                ).writeChannel()
            )
            response = r
        }.wait()
        return response!!
    }
}