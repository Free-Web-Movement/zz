package io.github.freewebmovement.zz.system.net.module

import io.github.freewebmovement.zz.system.net.IInstrumentedHandler
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.mainModule(execute: IInstrumentedHandler) {
    routing {
        get("/") {
            call.respondText("Hello From ZZ!\n")
        }
        route("/user") {
            get("/profile") {
                val responseStr = Json.encodeToString(execute.getProfile())
                call.respondText(responseStr)
            }
        }
    }
}