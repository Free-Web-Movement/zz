package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.system.net.module
import io.ktor.server.testing.testApplication
import org.junit.Test

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import junit.framework.TestCase.assertEquals


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class ServerUnitTest {
	@Test
	fun testRoot(): Unit = testApplication {
		application {
			module()
		}

		val response = client.get("/")
		assertEquals(HttpStatusCode.OK, response.status)
		assertEquals("Hello From ZZ!\n", response.bodyAsText())
	}
}