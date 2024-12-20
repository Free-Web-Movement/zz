package io.github.freewebmovement.zz.net

import android.os.Environment
import freemarker.cache.ClassTemplateLoader
import io.github.freewebmovement.zz.settings.Server
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.freemarker.FreeMarker
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.velocity.Velocity
import io.ktor.server.velocity.VelocityContent
import io.ktor.websocket.WebSocketDeflateExtension.Companion.install
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import java.lang.System.setProperty

const val DOWNLOAD_URI = "download"
fun Application.configureRouting() {

}

fun Application.module() {
//    configureRouting()
//    install(FreeMarker)

    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    routing {
        get("/") {
            //            if (IPList.hasPublicIP(Server.port)) {
//                val ipv6 = IPList.v6s(Server.port)
//                call.respond(
//                    VelocityContent(
//                        "templates/index.ftl", mapOf("port" to Server.port),
//                        mapOf("ip" to ipv6[0]).toString()
//                    ),
//                    typeInfo = TODO()
//                )
//            } else {
            call.respond(
                FreeMarkerContent("index.ftl", mapOf("user" to "ok"))
            )
//            call.respondText("No public IPV6 is not supported! \n ");
//            }
        }
        staticFiles(
            "/$DOWNLOAD_URI",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
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