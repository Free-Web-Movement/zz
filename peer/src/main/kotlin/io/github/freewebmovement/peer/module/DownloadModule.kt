package io.github.freewebmovement.peer.module

import io.github.freewebmovement.peer.interfaces.IShare
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing


fun Application.download(iDownload: IShare) {
    routing {
        staticFiles(
            "/download/statics",
            iDownload.downloadDir()
        )
        route("/download") {
            get("/apk") {
                call.respondFile(iDownload.myApk())
            }
        }
    }
}