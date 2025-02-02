package io.github.freewebmovement.peer

import io.github.freewebmovement.peer.database.entity.Peer
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

class PeerServer(app: IApp, peer: Peer) {
    private var server =
        embeddedServer(
            factory = Netty,
            port = peer.port,
            host = peer.ip
        ) {
            module(app)
        }.start(wait = false)

    fun stop() {
        server.stop()
    }

    fun restart() {
        server.start(wait = false)
    }
}