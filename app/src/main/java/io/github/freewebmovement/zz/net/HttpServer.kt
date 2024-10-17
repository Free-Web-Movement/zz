package io.github.freewebmovement.zz.net

import android.os.Environment
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello from Ktor!")
        }
//        get("/") {

        staticFiles("/d", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
//        }
    }
}

fun Application.module() {
    configureRouting()
}

class HttpServer {
    companion object {
        const val PORT = 10086;
        const val HOST = "0.0.0.0";
        fun start() {
            embeddedServer(
                Netty,
                port = PORT,
                host = HOST,
                module = Application::module
            ).start(wait = false)
        }
    }
}