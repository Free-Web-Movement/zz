package io.github.freewebmovement.zz

import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.system.database.entity.AddressType
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.settings.Server
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ServerInstrumentedTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun should_test_key_pair() {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val app = ApplicationProvider.getApplicationContext<MainApplication>()
            val preference = app.preference
            val setting = Settings(preference)
            PeerServer.start(app, Server.host, setting.localServerPort)
            val timeStamp = System.currentTimeMillis() / 1000
            val peerServer = Peer(
                Server.host,
                setting.localServerPort,
                AddressType.IPV4,
                createdAt = timeStamp, updatedAt = timeStamp
            )
            val peerClient = PeerClient(app, peerServer)

            val response01 = peerClient.client.get("/")
            assertEquals(HttpStatusCode.OK, response01.status)
            assertEquals("Hello From ZZ!\n", response01.bodyAsText())

            val response02 = peerClient.getPublicKey()
            assertEquals(HttpStatusCode.OK, response02.status)
            println(peerClient.server.rsaPublicKey)
            assertEquals(
                peerClient.server.rsaPublicKey,
                MainApplication.instance!!.crypto.publicKey.encoded.toHexString()
            )

            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                app.applicationContext.packageName
            );
            file.delete()
            assertEquals(file.exists(), false)
            val response03 = peerClient.getApkFile()
            assertEquals(file.exists(), true)
            assertEquals(HttpStatusCode.OK, response03.status)

            val response04 = peerClient.setPublicKey()
            assertEquals(HttpStatusCode.OK, response04.status)
        }
    }
}