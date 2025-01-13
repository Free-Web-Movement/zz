package io.github.freewebmovement.zz.system.net.api

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.mainModule() {
    routing {
        get("/") {
            call.respondText("Hello From ZZ!\n")
        }
    }
}