package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.bussiness.IDownload
import io.github.freewebmovement.zz.system.database.entity.Message
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.api.RoomHandler
import io.github.freewebmovement.zz.system.net.api.api
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.net.api.download
import io.github.freewebmovement.zz.system.net.api.mainModule
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
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

class ServerUnitTest {
    @Test
    fun testRoot() = testApplication {
        application {
            mainModule()
            download(TestDownload())
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


    }


    @Test
    fun testAPI() = testApplication {
        val crypto = Crypto.createCrypto()
        class MyRoomHandler: RoomHandler {
            override fun addPeer(peer: Peer) {
                TODO("Not yet implemented")
            }

            override fun getPeerBySessionId(id: String): Peer {
                TODO("Not yet implemented")
            }

            override fun addMessage(message: Message) {
                TODO("Not yet implemented")
            }

        }
        application {
            api(crypto, MyRoomHandler())
        }
        val response = client.get("/api/key/public")

//        val response02 = client.get("/download/statics")
//        assertEquals(HttpStatusCode.OK, response02.status)


    }
}