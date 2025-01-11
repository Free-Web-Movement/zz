package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.github.freewebmovement.zz.system.persistence.PeerSessionStorage
import io.ktor.server.application.Application
import io.ktor.server.application.install
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
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class PeerSession(val ip: String, val port: Int)


@OptIn(ExperimentalStdlibApi::class)
fun Application.module() {
	install(Sessions) {
		cookie<PeerSession>("peer_session", PeerSessionStorage()) {
			cookie.path = "/"
			cookie.maxAgeInSeconds = 10
		}
	}
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
				val publicKey = PublicKeyJSON(PeerServer.app.crypto.publicKey.encoded.toHexString())
				call.respond(publicKey)
			}
			post("/key/public") {
				val publicKey = call.receive<PublicKeyJSON>()
				call.respond({})
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