package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.system.settings.Server
import junit.framework.TestCase.assertTrue
import org.junit.Test

class IPUnitTest {
	@Test
	fun should_test_ip() {
		val ipList: IPList = IPList.getInstance(Server.port, true)
		assertTrue(ipList.ipv6IPs.isNotEmpty())
		assertTrue(ipList.ipv4IPs.isNotEmpty())
		IPList.reset()
		val ipListPublic: IPList = IPList.getInstance(Server.port)
		// not reliable due to the networks
		// tests tend to be failed, should ignore such failures
//		assertTrue(ipListPublic.ipv6IPs.isNotEmpty())
		assertTrue(ipListPublic.ipv4IPs.isEmpty())
	}
}