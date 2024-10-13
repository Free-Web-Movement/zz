package io.github.freewebmovement.zz.net

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello from Ktor!")
        }
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