package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.settings.Server
import io.ktor.http.HttpStatusCode
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerInstrumentedTest {
	@OptIn(ExperimentalStdlibApi::class)
	@Test
	fun should_test_key_pair() {
		val coroutineScope = CoroutineScope(Dispatchers.IO)
		coroutineScope.launch {
			val app = ApplicationProvider.getApplicationContext<MainApplication>()
			PeerServer.start(Server.host, Server.port)
			val timeStamp = System.currentTimeMillis() / 1000
			val peer = Peer(createdAt = timeStamp, updatedAt = timeStamp)
			peer.ipAddress = Server.host
			peer.ipPort = Server.port
			val client = PeerClient(peer)
			val response = client.stepOneGetPublicKey()
			assertEquals(HttpStatusCode.OK, response.status)
			println(client.server.rsaPublicKey)
			assertEquals(client.server.rsaPublicKey, MainApplication.instance!!.crypto.publicKey.encoded.toHexString())
		}
	}
}