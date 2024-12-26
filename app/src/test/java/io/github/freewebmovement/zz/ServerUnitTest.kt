package io.github.freewebmovement.zz

import io.github.freewebmovement.zz.net.module
import io.ktor.server.testing.testApplication
import org.junit.Test

import kotlin.test.assertEquals
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ServerUnitTest {
	@Test
	fun testRoot() = testApplication {
		application {
			module()
		}
		val response = client.get("/")
		assertEquals(HttpStatusCode.OK, response.status)
		assertEquals("Hello Android!\n", response.bodyAsText())
	}
}