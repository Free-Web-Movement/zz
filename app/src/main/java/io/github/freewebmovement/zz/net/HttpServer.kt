package io.github.freewebmovement.zz.net

import android.os.Environment
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

const val DOWNLOAD_URI = "download"

//fun Application.html(uri: List<String>) {
//
//}

fun Application.configureRouting() {
    routing {
        get("/") {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString())
            val files = directory.listFiles()
            val lines = ArrayList<String>()
            if (files != null) {
                for( i in files.indices) {
                    lines.add(files[i].name)
                }
            }
            call.respondText("Hello from Ktor! \n " + lines.joinToString(separator="\n"))
        }
        staticFiles(
            "/$DOWNLOAD_URI",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
    }
}

fun Application.module() {
    configureRouting()
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