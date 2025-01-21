package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.bussiness.IDownload
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.system.database.entity.Account
import io.github.freewebmovement.zz.system.database.entity.IPType
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.api.IInstrumentedHandler
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
import java.security.PublicKey
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
        var accountList = ArrayList<Account>()
        var peerList = ArrayList<Peer>()
        var messageList = ArrayList<Message>()
    }

    var avatar = ""
    var nickname = "nickname"
    var intro = "intro"
    override suspend fun addAccount(account: Account) {
        var id = 0
        accountList.forEach {
            if (it.id > id) {
                id = it.id
            }
        }
        account.id = id + 1
        accountList.add(account)
    }

    override suspend fun updateAccount(account: Account) {
        accountList.forEachIndexed { i, it ->
            if (it.id == account.id) {
                accountList[i] = account
                return@forEachIndexed
            }
        }
    }

    override suspend fun initPeer(ip: String, port: Int, ipType: IPType) {
        TODO("Not yet implemented")
    }

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

    override suspend fun getAccountByAddress(address: String): Account? {
        var found: Account? = null
        accountList.forEachIndexed { i, it ->
            if (it.address == address) {
                found = accountList[i]
                return@forEachIndexed
            }
        }
        return found
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
        json.type = IPType.IPV4
        return json
    }

    override fun getDownloadDir(): File {
        return File(".")
    }

    override fun getProfile(): UserJSON {
        return UserJSON(nickname = nickname, intro = intro, avatar = avatar)
    }

    override fun getCrypto(): Crypto {
        return crypto
    }

    override fun decrypt(enc: String): String {
        return Crypto.decrypt(enc, crypto.privateKey)
    }

    override fun encrypt(dec: String, account: Account?): String {
        if (account == null) {
            return Crypto.encrypt(dec, crypto.publicKey)
        }
//        val rsaPublicKey = peer.rsaPublicKeyByteArray.let { Crypto.revokePublicKey(it.toByteArray()) }
        return Crypto.encrypt(dec, Crypto.toPublicKey(account.publicKey))
    }

    override fun sign(message: String): ByteArray {
        return Crypto.sign(message.toByteArray(), crypto.privateKey)
    }

    override fun verify(message: String, signature: ByteArray, publicKey: PublicKey): Boolean {
        return Crypto.verify(message.toByteArray(), signature, publicKey)
    }

    override fun accessVerify(code: String, peer: Peer, to: Account) {
//        val httpScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
//        val request = httpScope.launch {
//            // suspend calls are allowed here cause this is a coroutine
//            app.peerClient.verifyAccessibility(code, peer, to)
//        }
    }
}

class ServerUnitTest {
    @Test
    fun testRoot() = testApplication {

        val serverCrypto = Crypto.createCrypto()
        val clientCrypto = Crypto.createCrypto()
        val serverHandler = APIHandler(serverCrypto)
        val clientHandler = APIHandler(clientCrypto)
        application {
            mainModule(serverHandler)
            download(TestDownload())
            api(serverHandler)
        }

        val serverAddress = Crypto.toAddress(serverCrypto.publicKey)
        val serverAccount = Account(serverAddress)
        serverAccount.publicKey = hex(serverCrypto.publicKey.encoded)
        serverHandler.addAccount(serverAccount)
        assert(serverAccount.id != 0)

        val clientAddress = Crypto.toAddress(clientCrypto.publicKey)
        val clientAccount = Account(clientAddress)
        clientAccount.publicKey = hex(clientCrypto.publicKey.encoded)

        clientHandler.addAccount(clientAccount)
        assert(clientAccount.id != 0)

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
        val peerServer = Peer(
//            serverAccount.id,
            "127.0.0.1", 0, IPType.IPV4)
        peerServer.id = 1
//        account.publicKey = hex(serverHandler.getCrypto().publicKey.encoded)
        peerServer.isTesting = true
        val peerClient = PeerClient(client, clientHandler)
        runBlocking {
            val publicKey = peerClient.getPublicKey(peerServer)
            assert(publicKey == serverCrypto.publicKey)
//            val address = Crypto.toAddress(serverCrypto.publicKey)
//            val account = clientHandler.getAccountByAddress(address)
        }

        runBlocking {
            val response04 = peerClient.setPublicKey(peerServer, serverAccount)
            assertEquals(HttpStatusCode.OK, response04.status)
            assert(APIHandler.peerList.size == 1)
            assert(APIHandler.messageList.size == 0)
        }

        runBlocking {
            val str = "Hello world!"
            val response04 = peerClient.sendMessage(str, peerServer, clientAccount, serverAccount)
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
            val response04 = peerClient.getProfile(peerServer, serverAccount)
            assert(serverAccount.avatar == serverHandler.avatar)
            assert(serverAccount.nickname == serverHandler.nickname)
            assert(serverAccount.intro == serverHandler.intro)
            assertEquals(HttpStatusCode.OK, response04.status)
        }
    }
}