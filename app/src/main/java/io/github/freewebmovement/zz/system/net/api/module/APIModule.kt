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

fun Application.api(execute: IInstrumentedHandler) {
    routing {
        route("/api") {
            get("/key/public") {
                val publicKey = execute.getPublicKeyJSON(true)
                call.respondText(Json.encodeToString(publicKey))
            }

            post("/key/public") {
                val jsonStr = call.receive<String>()
                val json = Json.decodeFromString<PublicKeyJSON>(jsonStr)
                val timeStamp = Time.now()
                val account = Account(json.key!!)
                execute.addAccount(account)
                val peer = Peer(
                    account.id,
                    ip = json.ip!!,
                    port = json.port!!,
                    ipType = json.type!!,
                    createdAt = timeStamp,
                    updatedAt = timeStamp
                )
                execute.addPeer(peer)
                call.respondText(Json.encodeToString(PublicKeyJSON()))
            }

            post("/message") {
                val receiveStr = call.receive<String>()
                val json = Json.decodeFromString<MessageSenderJSON>(receiveStr)

                val account: Account? = execute.getAccountByAddress(json.sender!!)
                val address = Crypto.toAddress(execute.getCrypto().publicKey)
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
                    val messageReceiverJSON = MessageReceiverJSON(Time.now())
                    call.respondText(Json.encodeToString(messageReceiverJSON))
                } else {
                    val messageReceiverJSON = MessageReceiverJSON(Time.now(), 1)
                    call.respondText(Json.encodeToString(messageReceiverJSON))
                }
            }
        }
    }
}