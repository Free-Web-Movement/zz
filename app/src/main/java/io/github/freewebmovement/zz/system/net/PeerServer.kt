package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.generateSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalStdlibApi::class)
fun Application.module() {
	routing {
		get("/") {
			call.respondText("Hello From ZZ!\n")
		}

		route("/download") {
			staticFiles(
				"/static",
				PeerServer.app.share.downloadDir()
			)
		}

		route("/app") {
			get("/download/apk") {
				call.respondFile(PeerServer.app.share.myApk())
			}
		}

		route("/api") {
			get("/key/public") {
				val publicKey = PublicKeyJSON(
					rsaPublicKeyByteArray = PeerServer.app.crypto.publicKey.encoded.toHexString()
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
				val peer = Peer(address = decJSON.ip!!, port = decJSON.port!!,
					addressType = decJSON.type!!,
					createdAt = timeStamp,
					updatedAt = timeStamp
					)
				peer.sessionId = sessionId
				PeerServer.app.db.peer().add(peer)
				val encStr01 = Crypto.encrypt(Json.encodeToString(PublicKeyJSON(sessionId = peer.sessionId)), PeerServer.app.crypto.publicKey)
				call.respondText(encStr01)
			}
		}
	}
}

class PeerServer {

	companion object {
		lateinit var app: MainApplication
		private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
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