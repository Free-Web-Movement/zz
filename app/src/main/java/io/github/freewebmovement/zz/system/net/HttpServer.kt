package io.github.freewebmovement.zz.system.net

import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing


const val DOWNLOAD_URI = "download"
@OptIn(ExperimentalStdlibApi::class)
fun Application.module() {
	routing {
		get("/") {
			call.respondText("Hello Android!\n")
		}

//		staticFiles(
//			"/$DOWNLOAD_URI",
//			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//		)

		route("/key") {
			get("/public") {
				val publicKey = HashMap<String, String>()
				publicKey["rsa_public_key"] = MainApplication.instance!!.crypto.publicKey.encoded.toHexString()
				call.respond(publicKey)
			}

			get("/private") {
				call.respondText (MainApplication.instance!!.crypto.privateKey.encoded.toHexString())
			}
		}
	}
}

class HttpServer {
	companion object {
		fun start(host: String = "0.0.0.0", port: Int = 10086) {
			embeddedServer(
				Netty,
				port = port,
				host = host,
				module = Application::module
			).start(wait = false)
		}
	}
}