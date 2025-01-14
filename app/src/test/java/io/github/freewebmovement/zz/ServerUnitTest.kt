package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.bussiness.IDownload
import io.github.freewebmovement.zz.system.database.entity.AddressType
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.api.api
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.download
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.github.freewebmovement.zz.system.net.api.mainModule
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.util.cio.writeChannel
import io.ktor.util.hex
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals


class TestDownload : IDownload {
    override fun myApk(): File {
        Paths.get("./test.txt").toAbsolutePath().toString()
        return File("test.txt")
    }

    override fun downloadDir(): File {
        return File("./")
    }
}

class APIHandler(private var crypto: Crypto) : IInstrumentedHandler {
    private var peerList = ArrayList<Peer>()
    private var messageList = ArrayList<Message>()

    override suspend fun addPeer(peer: Peer) {
        peerList.add(peer)
    }

    override suspend fun updatePeer(peer: Peer) {
        peerList.forEachIndexed { i, it ->
            if (it.id == peer.id) {
                peerList[i] = peer
                return@forEachIndexed
            }
        }
    }

    override suspend fun getPeerBySessionId(sessionId: String): Peer {
        var peer: Peer? = null
        peerList.forEachIndexed { i, it ->
            if (it.sessionId == sessionId) {
                peer = peerList[i]
                return@forEachIndexed
            }
        }
        return peer!!
    }

    override suspend fun addMessage(message: Message) {
        messageList.add(message)

    }

    override suspend fun updateMessage(message: Message) {
        messageList.forEachIndexed { i, it ->
            if (it.id == message.id) {
                messageList[i] = message
                return@forEachIndexed
            }
        }
    }

    override suspend fun getPublicKeyJSON(keyOnly: Boolean): PublicKeyJSON {
        val json = PublicKeyJSON(hex(crypto.publicKey.encoded))
        if (keyOnly) return json
        json.ip = "127.0.0.1"
        json.port = 0
        json.type = AddressType.IPV4
        return json
    }

    override fun getDownloadDir(): File {
        return File(".")
    }

    override fun getCrypto(): Crypto {
        return crypto
    }

    override fun decrypt(enc: String): String {
        return Crypto.decrypt(enc, crypto.privateKey)
    }

    override fun encrypt(dec: String, peer: Peer): String {
        val rsaPublicKey = Crypto.revokePublicKey(peer.rsaPublicKeyByteArray.toByteArray())
        return Crypto.encrypt(dec, rsaPublicKey)
    }

}

class ServerUnitTest {
    @Test
    fun testRoot() = testApplication {
        val handler = APIHandler(Crypto.createCrypto())
        application {
            mainModule()
            download(TestDownload())
            api(handler)
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello From ZZ!\n", response.bodyAsText())

        val file = File("newTest.txt")
        val response01 = client.get("/download/apk")
        response01.bodyAsChannel().copyAndClose(file.writeChannel())
        assertEquals(HttpStatusCode.OK, response01.status)
        val str = file.readText()
        assertEquals(str, "Test APK")

        file.delete()

//        val response02 = client.get("/download/statics")
//        assertEquals(HttpStatusCode.OK, response02.status)
//        val peerServer = Peer("127.0.0.1", 0, AddressType.IPV4, Time.now(), Time.now())
//        peerServer.isTesting = true
//        val peerClient = PeerClient(client, handler)
//        val response03 = peerClient.getPublicKey(peerServer)

        runBlocking {
            val response03 = client.get("/api/key/public")
            val jsonStr = response03.bodyAsText()
            val json = Json.decodeFromString<PublicKeyJSON>(jsonStr)
            json.rsaPublicKeyByteArray?.let { assert(it.isNotEmpty()) }
            assertEquals(HttpStatusCode.OK, response03.status)
        }

        val response04 = client.post("/api/key/public") {
            setBody("")
        }
        assertEquals(HttpStatusCode.OK, response04.status)
    }
}