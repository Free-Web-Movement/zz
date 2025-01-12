package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.system.Time
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.database.entity.AddressType
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.PeerServer
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerInstrumentedTest {
    @Test
    fun should_test_server_client() = runTest {
        val app = ApplicationProvider.getApplicationContext<MainApplication>()
        val preference = app.preference
        val setting = Settings(preference)
        setting.localServerPort = 0
        val db = ZzDatabase.getDatabase(app.applicationContext)
        db.peer().clearData()
        val port = setting.localServerPort
        PeerServer.start(app, "127.0.0.1", port)
        val timeStamp = Time.now()
        val peer = Peer(
            "127.0.0.1",
            port,
            AddressType.IPV4,
            createdAt = timeStamp, updatedAt = timeStamp
        )

        runBlocking {
            val peerClient = PeerClient(app, peer)
            val response01 = peerClient.client.get("http://www.baidu.com/")
//            val response01 = peerClient.client.get(peer.baseUrl + "/")
            assertEquals(HttpStatusCode.OK, response01.status)
            assertEquals("Hello From ZZ!\n", response01.bodyAsText())
        }


//            val response02 = peerClient.getPublicKey()
//            assertEquals(HttpStatusCode.OK, response02.status)
//            assertEquals(
//                peerClient.peer.rsaPublicKeyByteArray,
//                hex(MainApplication.instance!!.crypto.publicKey.encoded)
//            )
//
//            val file = File(
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//                app.applicationContext.packageName
//            )
//            file.delete()
//            assertEquals(file.exists(), false)
//            val response03 = peerClient.getApkFile()
//            assertEquals(file.exists(), true)
//            assertEquals(HttpStatusCode.OK, response03.status)
//
//            val response04 = peerClient.setPublicKey()
//            assertEquals(HttpStatusCode.OK, response04.status)
//            assert(db.peer().getAll().size  == 2)
//
//            val response05 = peerClient.sendMessage("Hello")
//            assertEquals(HttpStatusCode.OK, response05.status)
//            assert(db.message().getAll().size  == 2)
//            val messages = db.message().getAll()
//            println(messages[0].message)
//            assert(false)
    }
}