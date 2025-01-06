package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoInstrumentedTest {
	@Test
	fun should_test_key_pair() {
		val coroutineScope = CoroutineScope(Dispatchers.IO)
		coroutineScope.launch {
			val app = ApplicationProvider.getApplicationContext<MainApplication>()
			val crypto = Crypto.getInstance(app.preference)
			val str = "Hello World!"
			val enc = crypto.encrypt(str)
			val decStr = crypto.decrypt(enc)
			assertEquals(str, decStr)
			Crypto.refresh(app.preference)
		}
	}
}