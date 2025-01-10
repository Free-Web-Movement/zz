package io.github.freewebmovement.zz.system.net

import android.content.pm.PackageManager
import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File


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
				val publicKey = HashMap<String, String>()
				publicKey["rsaPublicKey"] = PeerServer.app.crypto.publicKey.encoded.toHexString()
				call.respond(publicKey)
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
				Netty,
				port = port,
				host = host,
				module = Application::module
			).start(wait = false)
		}
	}
}