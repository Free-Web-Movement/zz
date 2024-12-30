package io.github.freewebmovement.zz.system.net

import io.github.freewebmovement.zz.system.database.entity.Peer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class PublicKeyJSON(
	var rsa_public_key: String
)

class PeerClient(peerServer: Peer) {
	var server: Peer = peerServer
	private var client: HttpClient = HttpClient(CIO) {
		install(ContentNegotiation) {
			json()
		}
	}

	private fun getURI(path: String): String {
		if (server.isIpv6) {
			return "http://[${server.ipAddress}]:${server.ipPort}" + path
		}
		return "http://${server.ipAddress}:${server.ipPort}" + path
	}

	suspend fun stepOneGetPublicKey(): HttpResponse {
		val response = client.get(getURI("/api/key/public"))
		val json = response.body<PublicKeyJSON>()
		server.rsaPublicKey = json.rsa_public_key
		return response
	}
}