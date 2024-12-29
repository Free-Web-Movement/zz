package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.net.api.crypto.Crypto
import io.github.freewebmovement.zz.persistence.Preference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoUnitTest {
	@Test
	fun should_test_key_pair() {
		GlobalScope.launch {
			val app = ApplicationProvider.getApplicationContext<MainApplication>()
			val preference = Preference(app.baseContext)
			val crypto = Crypto.getInstance(preference)
			val str = "Hello World!"
			val enc = crypto.encrypt(str)
			val decStr = crypto.decrypt(enc)
			assertEquals(str, decStr)
		}
	}
}