package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.system.database.entity.Peer
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.persistence.Preference
import io.github.freewebmovement.zz.system.settings.Server
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerInstrumentedTest {
	@Test
	fun should_test_key_pair() {
		val coroutineScope = CoroutineScope(Dispatchers.IO)
		coroutineScope.launch {
			val app = ApplicationProvider.getApplicationContext<MainApplication>()
			Preference(app.baseContext)
			PeerServer.start(Server.host, Server.port)
			val timeStamp = System.currentTimeMillis() / 1000
			val peer = Peer(createdAt = timeStamp, updatedAt = timeStamp);
			peer.ipAddress = Server.host
			peer.ipPort = Server.port
			val client: PeerClient = PeerClient(peer)
			val response = client.stepOneGetPublicKey()
			assertEquals(HttpStatusCode.OK, response.status)
			println(client.server.rsaPublicKey)
			assertEquals(client.server.rsaPublicKey, PeerServer.publicKey)
		}
	}
}