package io.github.freewebmovement.zz.system.net.module

import io.github.freewebmovement.system.Time
import io.github.freewebmovement.system.crypto.Crypto
import io.github.freewebmovement.peer.json.MessageReceiverJSON
import io.github.freewebmovement.peer.json.MessageSenderJSON
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.SignJSON
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.signType
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalStdlibApi::class)
fun Application.api(execute: IInstrumentedHandler) {
    routing {
        route("/api") {
            get("/key/public") {
                val publicKey = execute.getPublicKeyJSON(true)
                val json = Json.encodeToString(publicKey)
                val signature = execute.sign(json).toHexString()
                call.respondText(Json.encodeToString(SignJSON(json, signature)))
            }

            post("/key/public") {
                val jsonStr = call.receive<String>()
                val signJSON = Json.decodeFromString<SignJSON>(jsonStr)
                val json = Json.decodeFromString<PublicKeyJSON>(signJSON.json)
                val publicKey = Crypto.toPublicKey(json.key!!)
                assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(),
                    publicKey))
                val address = Crypto.toAddress(publicKey)
                var account = Account(address)
                account.publicKey = json.key!!
                execute.addAccount(account)
                account = execute.getAccountByAddress(address)!!
                val peer = Peer(
                    ip = json.ip!!,
                    port = json.port!!,
                    ipType = json.type!!,
                )
                peer.account = account.id
                peer.accessibilityVerified = true
                execute.addPeer(peer)
                call.respondText(execute.signType(PublicKeyJSON()))
                execute.accessVerify(json.accessibilityVerificationCode!!, peer, account)
            }


            post("/code") {
                val jsonStr = call.receive<String>()
                val signJSON = Json.decodeFromString<SignJSON>(jsonStr)
                val json = Json.decodeFromString<PublicKeyJSON>(signJSON.json)
                val publicKey = Crypto.toPublicKey(json.key!!)
                assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(),
                    publicKey))
                val address = Crypto.toAddress(publicKey)
                val peer = execute.getPeerByCode(json.accessibilityVerificationCode!!)
                peer.accessibilityVerified = true
                peer.latestSeen = Time.now()
                execute.updatePeer(peer)
                call.respondText(execute.signType(PublicKeyJSON()))
            }


            post("/message") {
                val receiveStr = call.receive<String>()
                val signJSON = Json.decodeFromString<SignJSON>(receiveStr)
                val json = Json.decodeFromString<MessageSenderJSON>(signJSON.json)
                val account: Account = execute.getAccountByAddress(json.sender)!!
                val address = Crypto.toAddress(execute.getCrypto().publicKey)
                val sender = execute.getAccountByAddress(address)
                assert(sender != null)
                assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(),
                    Crypto.toPublicKey(account.publicKey)))

                val code = 0
                val message = Message(
                    isSending = false,
                    isSucceeded = true,
                    from = account.address,
                    to = address,
                    message = execute.decrypt(json.message),
                    createdAt = json.createdAt
                )
                message.receivedAt = Time.now()
                execute.addMessage(message)
                val messageReceiverJSON = MessageReceiverJSON(Time.now(), code)
                val toJson = Json.encodeToString(messageReceiverJSON)
                val sign = execute.sign(toJson).toHexString()
                call.respondText(Json.encodeToString(SignJSON(toJson, sign)))
            }
        }
    }
}