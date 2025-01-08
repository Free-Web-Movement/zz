package io.github.freewebmovement.zz

import android.app.Application.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.system.net.IPList
import junit.framework.TestCase.assertTrue
import org.junit.Test

class IPUnitTest {
	@Test
	fun should_test_ip() {
		val app = ApplicationProvider.getApplicationContext<MainApplication>()
		val preferences: SharedPreferences = app.baseContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		val settings = Settings(preferences)
		val ipList: IPList = IPList.getInstance(settings)
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