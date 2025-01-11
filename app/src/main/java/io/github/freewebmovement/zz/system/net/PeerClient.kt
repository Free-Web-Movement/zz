package io.github.freewebmovement.zz.system.net

import android.os.Environment
import io.github.freewebmovement.zz.MainApplication
import io.github.freewebmovement.zz.R
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import okhttp3.internal.wait
import java.io.File

@Suppress("PLUGIN_IS_NOT_ENABLED")

class PeerClient(var app: MainApplication, var server: Peer) {
    var client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Step 1. Initial step to get public key from a peer server
    suspend fun getPublicKey(): HttpResponse {
        val response = client.get(server.baseUrl + "/api/key/public")
        val json = response.body<PublicKeyJSON>()
        server.rsaPublicKey = json.rsaPublicKey
        return response
    }

    // Step 2. send your public key to the server
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun setPublicKey(): HttpResponse {
        val json = PublicKeyJSON(app.crypto.publicKey.encoded.toHexString())
        if (app.ipList.hasPublicIPs()) {
            json.ip = app.ipList.getPublicUri()
            json.port = app.settings.localServerPort
        } else {
            throw Exception(stringResource(R.string.share_app_apk_no_public_ip))
        }

        val response = client.post(server.baseUrl + "/api/key/public") {
            contentType(ContentType.Application.Json)
            setBody(json)
        }
        return response
    }



    /**
     * get apk file from server, for test only
     */
    suspend fun getApkFile() :  HttpResponse {
        var response: HttpResponse? = null
        val temp = client.prepareRequest {
            url(server.baseUrl + "/app/download/apk")
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