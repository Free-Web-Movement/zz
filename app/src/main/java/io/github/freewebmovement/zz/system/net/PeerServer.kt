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

class PeerServer(var app: MainApplication, var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>) {
    companion object {
        fun start(app: MainApplication, host: String, port: Int): PeerServer {
            val server = embeddedServer(
                factory = Netty,
                port = port,
                host = host
            ) {
                module(app)
            }.start(wait = false)
            return PeerServer(app, server)
        }
    }
}