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
import io.ktor.server.sessions.generateSessionId
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import okhttp3.internal.wait
import java.io.File

class PeerClient(var app: MainApplication, a: Peer) {
    val peer : Peer = a
    var client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Step 1. Initial step to get public key from a peer server
    suspend fun getPublicKey(): HttpResponse {
        val response = client.get(peer.baseUrl + "/api/key/public")
        val json = response.body<PublicKeyJSON>()
        peer.rsaPublicKey = json.rsaPublicKey.toString()
        return response
    }

    // Step 2. send your public key to the server
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun setPublicKey(): HttpResponse {
        val sessionId = generateSessionId()
        val json = PublicKeyJSON(app.crypto.publicKey.encoded.toHexString())
        if (app.ipList.hasPublicIPs()) {
            json.ip = app.ipList.getPublicUri()
            json.port = app.settings.localServerPort
            json.sessionId = sessionId
            peer.sessionId = sessionId
            app.db.peer().add(peer)
        } else {
            throw Exception(app.applicationContext.getString(R.string.share_app_apk_no_public_ip))
        }

        val response = client.post(peer.baseUrl + "/api/key/public") {
            contentType(ContentType.Application.Json)
            setBody(json)
        }
        val jsonRes = response.body<PublicKeyJSON>()
        jsonRes.sessionId?.let { assert(it.isNotEmpty()) }
        peer.peerSessionId = jsonRes.sessionId.toString()
        app.db.peer().update(peer)
        return response
    }



    /**
     * get apk file from server, for test only
     */
    suspend fun getApkFile() :  HttpResponse {
        var response: HttpResponse? = null
        val temp = client.prepareRequest {
            url(peer.baseUrl + "/app/download/apk")
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