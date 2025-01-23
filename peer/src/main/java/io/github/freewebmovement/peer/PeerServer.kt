package io.github.freewebmovement.peer

import io.github.freewebmovement.peer.interfaces.IApp
import io.github.freewebmovement.peer.module.api
import io.github.freewebmovement.peer.module.download
import io.github.freewebmovement.peer.module.mainModule
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun Application.module(app: IApp) {
    mainModule(app.handler)
    download(app.share)
    api(app.handler)
}

class PeerServer(var app: IApp) {
    companion object {
        private var server : Any? = null
        fun start(app: IApp, host: String, port: Int): PeerServer {
            server = embeddedServer(
                factory = Netty,
                port = port,
                host = host
            ) {
                module(app)
            }.start(wait = false)
            return PeerServer(app)
        }
    }
}