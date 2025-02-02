package io.github.freewebmovement.peer.module

import io.github.freewebmovement.peer.database.entity.Message
import io.github.freewebmovement.peer.json.MessagePollingReceiverJSON
import io.github.freewebmovement.peer.json.MessagePollingSenderJSON
import io.github.freewebmovement.peer.json.MessageReceiverJSON
import io.github.freewebmovement.peer.json.MessageSenderJSON
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.system.Time
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.interfaces.IInstrumentedHandler
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.api(execute: IInstrumentedHandler) {
    routing {
        route("/api") {
            get("/key/public") {
                val publicKey = execute.getPublicKeyJSON(true)
                call.respondText(Crypto.sign(publicKey, execute.getCrypto()))
            }

            post("/key/public") {
                val jsonStr = call.receive<String>()
                val json = Crypto.verify<PublicKeyJSON>(jsonStr)
                execute.initAccount(json)
                call.respondText(Crypto.sign(PublicKeyJSON(), execute.getCrypto()))
            }

            // sending Messages to peers having no public accessibility
            get("/message") {
                val receiveStr = call.receive<String>()
                val json = Crypto.verify<MessagePollingSenderJSON>(receiveStr)
                val publicKey = Crypto.fromHexString(json.sender)
                val address = Crypto.toAddress(publicKey)
                val messages  = execute.getMessagesByAddress(address)
                val messageList = ArrayList<io.github.freewebmovement.peer.json.Message>()
                messages.forEach {
                    val message = io.github.freewebmovement.peer.json.Message(
                        it.from,
                        it.to,
                        Crypto.encrypt(it.message, publicKey),
                        it.createdAt
                    )
                    messageList.add(message)
                }
                call.respondText(Crypto.sign(MessagePollingReceiverJSON(messageList, 0), execute.getCrypto()))
            }
            // Accept posted messages
            post("/message") {
                val receiveStr = call.receive<String>()
                val json = Crypto.verify<MessageSenderJSON>(receiveStr)
                val account = execute.getAccountByAddress(json.sender)!!
                val crypto = execute.getCrypto()
                val address = Crypto.toAddress(crypto.publicKey)
                val message = Message(
                    isSending = false,
                    isSucceeded = true,
                    from = account.address,
                    to = address,
                    message = Crypto.decrypt(json.message, crypto.privateKey),
                    createdAt = json.createdAt
                )
                message.receivedAt = Time.now()
                execute.addMessage(message)
                call.respondText(Crypto.sign(MessageReceiverJSON(Time.now(), 0), execute.getCrypto()))
            }
        }
    }
}