package io.github.freewebmovement.zz.net
import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.velocity.Velocity
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader

const val DOWNLOAD_URI = "download"
fun Application.module() {

//    install(FreeMarker) {
//        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
//        println("templateLoader = $templateLoader")
//    }

    install(Velocity) {
//        val file = MainApplication.instance!!.applicationContext.filesDir.absolutePath;
        val app = MainApplication.instance!!
//        val file = app.dataDir.absoluteFile
        val path = app.packageResourcePath
        val path1 = app.packageCodePath
        val path2 = app.getFileStreamPath(path1)
        //(R.raw.anchor);
        println("inside volocity");
        println(RuntimeConstants.RESOURCE_LOADER);
        println(ClasspathResourceLoader::class.java.name);
//        println(file);
        println(path);
        println(path1);
        println(path2);
        println("end volocity");

        setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
        setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
        setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, path)
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
          //  call.respond(VelocityContent("templates/index.vl", mapOf("user" to "")))
            call.respondText("Hello Android! \n ");
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