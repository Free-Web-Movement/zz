package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.net.IPList
import org.junit.Test

import kotlin.random.Random

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class IPUnitTest {
	@Test
	fun should_test_public_ip() {
		val port = Random(100).nextInt(10000) + 1024
		val result = IPList.hasPublicIP(port);
		val result1 = IPList.hasPublicIP(port);
		println(result);
	}
}