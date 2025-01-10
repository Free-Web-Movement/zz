package io.github.freewebmovement.zz.system.net

import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.Serializable
import okhttp3.internal.wait
import java.io.File

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class PublicKeyJSON(
    var rsaPublicKey: String
)

class PeerClient(var app: MainApplication, var server: Peer) {
    var client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Step 1.
    suspend fun getPublicKey(): HttpResponse {
        val response = client.get(server.base_url + "/api/key/public")
        val json = response.body<PublicKeyJSON>()
        server.rsaPublicKey = json.rsaPublicKey
        return response
    }

    suspend fun getApkFile() :  HttpResponse {
        var response: HttpResponse? = null
        var temp = client.prepareRequest {
            url(server.base_url + "/app/download/apk")
        }
        temp.execute { r ->
            r.bodyAsChannel().copyAndClose(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    app.applicationContext.packageName
                ).writeChannel()
            )
            response = r
        }.wait()
        return response!!
    }
}