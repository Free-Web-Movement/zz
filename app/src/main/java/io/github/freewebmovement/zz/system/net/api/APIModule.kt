package io.github.freewebmovement.zz.system.net.api

import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.json.MessagReceiverJSON
import io.github.freewebmovement.zz.system.net.api.json.MessageSenderJSON
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.generateSessionId
import io.ktor.util.hex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.api(execute: IInstrumentedHandler) {
    routing {
        route("/api") {
            get("/key/public") {
                call.respondText("Hello From Key Public GET!\n")
//                val publicKey = execute.getPublicKeyJSON(true);
//                call.respond(publicKey)

//                val publicKey = PublicKeyJSON(
//                    rsaPublicKeyByteArray = hex(execute.getCrypto().publicKey.encoded)
//                )
//                call.respond(publicKey)
            }

            post("/key/public") {
                call.respondText("Hello From Key Public POST!\n")

//                val sessionId = generateSessionId()
//                val encStr = call.receive<String>()
//                val decStr = Crypto.decrypt(encStr, execute.getCrypto().privateKey)
//                val decJSON = Json.decodeFromString<PublicKeyJSON>(decStr)
//                val timeStamp = Time.now()
//                assert(decJSON.ip!!.isNotEmpty())
//                assert(decJSON.port!! > 1 shl 10)
//                val peer = Peer(
//                    address = decJSON.ip!!, port = decJSON.port!!,
//                    addressType = decJSON.type!!,
//                    createdAt = timeStamp,
//                    updatedAt = timeStamp
//                )
//                peer.sessionId = sessionId
//                execute.addPeer(peer)
////                PeerServer.app.db.peer().add(peer)
//                val encStr01 = Crypto.encrypt(
//                    Json.encodeToString(PublicKeyJSON(sessionId = peer.sessionId)),
//                    execute.getCrypto().publicKey
//                )
//                call.respondText(encStr01)
            }

            post("/message") {
                val encStr = call.receive<String>()
                val decStr = Crypto.decrypt(encStr, execute.getCrypto().privateKey)
                val decJSON = Json.decodeFromString<MessageSenderJSON>(decStr)

//                val peer = decJSON.sessionId?.let { PeerServer.app.db.peer().getBySessionId(it) }
                val peer: Peer = execute.getPeerBySessionId(decJSON.sessionId!!)
                val message = Message(
                    isSending = false,
                    isSucceeded = true,
                    peer = peer.id,
                    message = decJSON.message,
                    createdAt = decJSON.createdAt
                )
                message.receivedAt = Time.now()
                execute.addMessage(message)
//                    PeerServer.app.db.message().add(message)

                val messageReceiverJSON = MessagReceiverJSON(Time.now())
                val res = Crypto.encrypt(
                    Json.encodeToString(messageReceiverJSON),
                    PeerServer.app.crypto.publicKey
                )
                call.respondText(res)
            }
        }
    }
}