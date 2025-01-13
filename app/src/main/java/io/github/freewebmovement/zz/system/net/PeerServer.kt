package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.download
import io.github.freewebmovement.zz.system.net.api.json.MessagReceiverJSON
import io.github.freewebmovement.zz.system.net.api.json.MessageSenderJSON
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.github.freewebmovement.zz.system.net.api.mainModule
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.generateSessionId
import io.ktor.util.hex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.module() {
    mainModule()
    download(PeerServer.app.share)
    routing {
//        route("/download") {
//            staticFiles(
//                "/static",
//                PeerServer.app.share.downloadDir()
//            )
//        }
//
//        route("/app") {
//            get("/download/apk") {
//                call.respondFile(PeerServer.app.share.myApk())
//            }
//        }

        route("/api") {
            get("/key/public") {
                val publicKey = PublicKeyJSON(
                    rsaPublicKeyByteArray = hex(PeerServer.app.crypto.publicKey.encoded)
                )
                call.respond(publicKey)
            }
            post("/key/public") {
                val sessionId = generateSessionId()
                val encStr = call.receive<String>()
                val decStr = Crypto.decrypt(encStr, PeerServer.app.crypto.privateKey)
                val decJSON = Json.decodeFromString<PublicKeyJSON>(decStr)
                val timeStamp = Time.now()
                assert(decJSON.ip!!.isNotEmpty())
                assert(decJSON.port!! > 1 shl 10)
                val peer = Peer(
                    address = decJSON.ip!!, port = decJSON.port!!,
                    addressType = decJSON.type!!,
                    createdAt = timeStamp,
                    updatedAt = timeStamp
                )
                peer.sessionId = sessionId
                PeerServer.app.db.peer().add(peer)
                val encStr01 = Crypto.encrypt(
                    Json.encodeToString(PublicKeyJSON(sessionId = peer.sessionId)),
                    PeerServer.app.crypto.publicKey
                )
                call.respondText(encStr01)
            }

            post("/message") {
                val encStr = call.receive<String>()
                val decStr = Crypto.decrypt(encStr, PeerServer.app.crypto.privateKey)
                val decJSON = Json.decodeFromString<MessageSenderJSON>(decStr)

                val peer = decJSON.sessionId?.let { PeerServer.app.db.peer().getBySessionId(it) }
                if (peer != null) {
                    val message = Message(
                        isSending = false,
                        isSucceeded = true,
                        peer = peer.id,
                        message = decJSON.message,
                        createdAt = decJSON.createdAt
                    )
                    message.receivedAt = Time.now()
                    PeerServer.app.db.message().add(message)
                }

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

class PeerServer {

    companion object {
        lateinit var app: MainApplication
        private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
            null

        fun start(app: MainApplication, host: String = "0.0.0.0", port: Int = 10086) {
            PeerServer.app = app
            server = embeddedServer(
                factory = Netty,
                port = port,
                host = host,
                module = Application::module
            ).start(wait = false)
        }
    }
}