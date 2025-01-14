package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.bussiness.IDownload
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.entity.AddressType
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.api.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.json.PublicKeyJSON
import io.github.freewebmovement.zz.system.net.api.json.UserJSON
import io.github.freewebmovement.zz.system.net.api.module.api
import io.github.freewebmovement.zz.system.net.api.module.download
import io.github.freewebmovement.zz.system.net.api.module.mainModule
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.util.cio.writeChannel
import io.ktor.util.hex
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.runBlocking
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
    companion object {
        var peerList = ArrayList<Peer>()
        var messageList = ArrayList<Message>()
    }

    var avatar = ""
    var nickname = "nickname"
    var signature = "signature"

    override suspend fun addPeer(peer: Peer) {
        var id = 0
        peerList.forEach {
            if (it.id > id) {
                id = it.id
            }
        }
        peer.id = id + 1
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
        var id = 0
        messageList.forEach {
            if (it.id > id) {
                id = it.id
            }
        }
        message.id = id + 1
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

    override fun getProfile(): UserJSON {
        return UserJSON(nickname = nickname, signature = signature, avatar = avatar)
    }

    override fun getCrypto(): Crypto {
        return crypto
    }

    override fun decrypt(enc: String): String {
        return Crypto.decrypt(enc, crypto.privateKey)
    }

    override fun encrypt(dec: String, peer: Peer?): String {
        if (peer == null) {
            return Crypto.encrypt(dec, crypto.publicKey)
        }
//        val rsaPublicKey = peer.rsaPublicKeyByteArray.let { Crypto.revokePublicKey(it.toByteArray()) }
        return Crypto.encrypt(dec, Crypto.toPublicKey(peer.rsaPublicKeyByteArray))
    }

}

class ServerUnitTest {
    @Test
    fun testRoot() = testApplication {
        val serverHandler = APIHandler(Crypto.createCrypto())
        val clientHandler = APIHandler(Crypto.createCrypto())
        application {
            mainModule(serverHandler)
            download(TestDownload())
            api(serverHandler)
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello From ZZ!\n", response.bodyAsText())

        val file = File("newTest.txt")
        val response01 = client.get("/download/apk")
        response01.bodyAsChannel().copyAndClose(file.writeChannel())
        assertEquals(HttpStatusCode.OK, response01.status)
        val readText = file.readText()
        assertEquals(readText, "Test APK")

        file.delete()

//        val response02 = client.get("/download/statics")
//        assertEquals(HttpStatusCode.OK, response02.status)
        val peerServer = Peer("127.0.0.1", 0, AddressType.IPV4, Time.now(), Time.now())
        peerServer.id = 1
        peerServer.rsaPublicKeyByteArray = hex(serverHandler.getCrypto().publicKey.encoded)
        peerServer.isTesting = true
        val peerClient = PeerClient(client, clientHandler)
        runBlocking {
            val response003 = peerClient.getPublicKey(peerServer)
            assert(peerServer.rsaPublicKeyByteArray.isNotEmpty())
            assertEquals(HttpStatusCode.OK, response003.status)
        }

        runBlocking {
            assert(peerServer.sessionId.isEmpty())
            assert(peerServer.peerSessionId.isEmpty())
            val response04 = peerClient.setPublicKey(peerServer)
            assert(peerServer.sessionId.isNotEmpty())
            assert(peerServer.peerSessionId.isNotEmpty())
            assertEquals(HttpStatusCode.OK, response04.status)
            assert(APIHandler.peerList.size == 2)
            assert(APIHandler.messageList.size == 0)
        }

        runBlocking {
            val str = "Hello world!"
            val response04 = peerClient.sendMessage(str, peerServer)
            assert(APIHandler.messageList.size == 2)
            val message = APIHandler.messageList[0]
            val message01 = APIHandler.messageList[1]
            assert(message.message == str)
            assert(message01.message == str)
            assert(message.isSending)
            assert(!message01.isSending)
            assertEquals(HttpStatusCode.OK, response04.status)
        }

        runBlocking {
            val response04 = peerClient.getProfile(peerServer)
            assert(peerServer.avatar == serverHandler.avatar)
            assert(peerServer.nickname == serverHandler.nickname)
            assert(peerServer.signature == serverHandler.signature)
            assertEquals(HttpStatusCode.OK, response04.status)
        }
    }
}