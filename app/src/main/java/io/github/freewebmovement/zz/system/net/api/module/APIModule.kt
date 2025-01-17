package io.github.freewebmovement.zz.system.net.api.module

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
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.generateSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalStdlibApi::class)
fun Application.api(execute: IInstrumentedHandler) {
    routing {
        route("/api") {
            get("/key/public") {
                val publicKey = execute.getPublicKeyJSON(true)
                var json = Json.encodeToString(publicKey);
                var signature = execute.sign(json).toHexString()
                call.respondText(Json.encodeToString(SignJSON(json, signature)))
            }

            post("/key/public") {
                val jsonStr = call.receive<String>()
                val signJSON = Json.decodeFromString<SignJSON>(jsonStr)
                val json = Json.decodeFromString<PublicKeyJSON>(signJSON.json)
                val publicKey = Crypto.toPublicKey(json.key!!)
                assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(),
                    publicKey))
                val timeStamp = Time.now()
                val address = Crypto.toAddress(publicKey)
                var account = Account(address)
                account.publicKey = json.key!!
                execute.addAccount(account)
                account = execute.getAccountByAddress(address)!!
                val peer = Peer(
                    account.id,
                    ip = json.ip!!,
                    port = json.port!!,
                    ipType = json.type!!,
                    createdAt = timeStamp,
                    updatedAt = timeStamp
                )
                execute.addPeer(peer)
                var publicKeyJSON = Json.encodeToString(PublicKeyJSON())
                var sign = execute.sign(publicKeyJSON).toHexString()
                call.respondText(Json.encodeToString(SignJSON(publicKeyJSON, sign)))
            }

            post("/message") {
                val receiveStr = call.receive<String>()
                val signJSON = Json.decodeFromString<SignJSON>(receiveStr)
                val json = Json.decodeFromString<MessageSenderJSON>(signJSON.json)
                val account: Account = execute.getAccountByAddress(json.sender!!)!!
                val address = Crypto.toAddress(execute.getCrypto().publicKey)
                val sender = execute.getAccountByAddress(address)
                assert(sender != null)
                assert(execute.verify(signJSON.json, signJSON.signature.hexToByteArray(),
                    Crypto.toPublicKey(account.publicKey)))

                var code = 0
                if (account != null) {
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
                } else {
                    code = 1
                }
                val messageReceiverJSON = MessageReceiverJSON(Time.now(), code)
                var toJson = Json.encodeToString(messageReceiverJSON)
                var sign = execute.sign(toJson).toHexString()
                call.respondText(Json.encodeToString(SignJSON(toJson, sign)))
            }
        }
    }
}