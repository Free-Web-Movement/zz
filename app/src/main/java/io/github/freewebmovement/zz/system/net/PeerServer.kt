package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.net.api.RoomHandler
import io.github.freewebmovement.zz.system.net.api.module.api
import io.github.freewebmovement.zz.system.net.api.module.download
import io.github.freewebmovement.zz.system.net.api.module.mainModule
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine

fun Application.module(app: MainApplication) {
    mainModule(RoomHandler(app))
    download(app.share)
    api(RoomHandler(app))
}

class PeerServer {
    companion object {
        lateinit var app: MainApplication
        private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
            null

        fun start(app: MainApplication, host: String, port: Int) {
            PeerServer.app = app
            server = embeddedServer(
                factory = Netty,
                port = port,
                host = host
            ) {
                module(app)
            }.start(wait = false)
        }
    }
}