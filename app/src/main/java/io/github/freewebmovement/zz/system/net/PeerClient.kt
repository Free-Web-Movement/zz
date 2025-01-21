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
import io.github.freewebmovement.zz.system.net.api.json.SignJSON
import io.github.freewebmovement.zz.system.net.api.json.UserJSON
import io.github.freewebmovement.zz.system.net.api.signType
import io.github.freewebmovement.zz.system.net.api.verifyType
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
import io.ktor.util.hex
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.internal.wait
import java.security.PublicKey

class PeerClient(private var client: HttpClient, private var execute: IInstrumentedHandler) {
    // Step 1. Initial step to get public key from a peer server
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun getPublicKey(peer: Peer): PublicKey {
        val response = client.get(peer.baseUrl + "/api/key/public")
        val jsonStr = response.bodyAsText()

        val json = Json.decodeFromString<SignJSON>(jsonStr)
        val publicKeyJSON = Json.decodeFromString<PublicKeyJSON>(json.json)
        val publicKey = Crypto.toPublicKey(publicKeyJSON.key!!)
        assert(execute.verify(json.json, json.signature.hexToByteArray(), publicKey))
        val address = Crypto.toAddress(publicKey)
        val account = Account(address = address)
        account.publicKey = hex(publicKey.encoded)
        execute.addAccount(account)
        peer.account = account.id
        execute.addPeer(peer)
        return publicKey
    }

    // Step 2. send your public key to the server
    suspend fun setPublicKey(peer: Peer, publicKey: PublicKey): HttpResponse {
        val json = execute.getPublicKeyJSON()
        peer.accessibilityVerificationCode = peer.getCode
        json.accessibilityVerificationCode = peer.accessibilityVerificationCode
        execute.updatePeer(peer)
//        val jsonStr = Json.encodeToString(json)
//        val sign = execute.sign(jsonStr).toHexString()
        val response = client.post(peer.baseUrl + "/api/key/public") {
            setBody(execute.signType(json))
        }
        val resStr = response.body<String>()
//        val signJSON = Json.decodeFromString<SignJSON>(resStr)
//        assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(),
//            Crypto.toPublicKey(to.publicKey)))

//        val signJSON = execute.verifyType<SignJSON>(resStr, to)

//        val resJson = Json.decodeFromString<PublicKeyJSON>(signJSON.json)
        val resJson = execute.verifyType<PublicKeyJSON>(resStr, publicKey)
        assert(resJson.code == 0)
        peer.latestSeen = Time.now()
        execute.updatePeer(peer)
        return response
    }

    // Step 3. Verify if the client peer can be a server or not. This step switch to the Server peer
    //         Must not be executed in the same ip with step 1, 2
    suspend fun verifyAccessibility(code: String , peer: Peer, publicKey: PublicKey): HttpResponse {
        val json = execute.getPublicKeyJSON(true)
        json.accessibilityVerificationCode = code
        val response = client.post(peer.baseUrl + "/api/code") {
            setBody(execute.signType(json))
        }
        val resStr = response.body<String>()
        val resJson = execute.verifyType<PublicKeyJSON>(resStr, publicKey)
        assert(resJson.code == 0)
        peer.latestSeen = Time.now()
        execute.updatePeer(peer)
        return response
    }


        // Step 4. Now can start messaging from peer to peer
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun sendMessage(sendStr: String, peer: Peer, from: Account, to: Account): HttpResponse {
        // Sending your address as session id
        val messageJSON = MessageSenderJSON(
            from.address, "", Time.now())
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
        val json = Json.encodeToString(messageJSON)
        val sign = execute.sign(json).toHexString()

        val response = client.post(peer.baseUrl + "/api/message") {
            setBody(Json.encodeToString(SignJSON(json, sign)))
        }
        val receiverStr = response.body<String>()
        val signJSON = Json.decodeFromString<SignJSON>(receiverStr)
        assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(), Crypto.toPublicKey(to.publicKey)))
        val messageReceiverJSON = Json.decodeFromString<MessageReceiverJSON>(signJSON.json)
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
        account.intro = user.intro
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
            )
            response = r
        }.wait()
        return response!!
    }
}