package io.github.freewebmovement.zz.system.net.module

import io.github.freewebmovement.peer.IDownload
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing


fun Application.download(iDownload: IDownload) {
    routing {
        route("/download") {
            staticFiles(
                "/statics",
                iDownload.downloadDir()
            )

            get("/apk") {
                call.respondFile(iDownload.myApk())
            }
        }
    }
}