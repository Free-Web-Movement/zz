package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.system.settings.Server
import junit.framework.TestCase.assertTrue
import org.junit.Test

class IPUnitTest {
	@Test
	fun should_test_ip() {
		val ipList: IPList = IPList.getInstance(Server.port)
		assertTrue(ipList.ipv4IPLocal.isNotEmpty())
		assertTrue(ipList.ipv6IPLocal.isNotEmpty())
		assertTrue(ipList.ipv4IPPublic.size >= 0)
		assertTrue(ipList.ipv6IPPublic.size >= 0)
		assertTrue(ipList.toHTTPV4Uris(ipv4s = ipList.ipv4IPLocal).isNotEmpty())
		assertTrue(ipList.toHTTPV4Uris(ipv4s = ipList.ipv4IPPublic).size >= 0)
		assertTrue(ipList.toHTTPV6Uris(ipv6s = ipList.ipv6IPLocal).isNotEmpty())
		assertTrue(ipList.toHTTPV6Uris(ipv6s = ipList.ipv6IPPublic).size >= 0)
	}
}