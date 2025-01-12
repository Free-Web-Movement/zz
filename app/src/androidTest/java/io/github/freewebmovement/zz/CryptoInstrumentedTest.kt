package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoInstrumentedTest {
    @Test
    fun should_test_key_pair() = runTest {
        val app = ApplicationProvider.getApplicationContext<MainApplication>()
        val crypto = Crypto.getInstance(app.preference)
        val str = "Hello World!"
        val enc = Crypto.encrypt(str, crypto.publicKey)
        val decStr = Crypto.decrypt(enc, crypto.privateKey)
        assertEquals(str, decStr)
        Crypto.refresh(app.preference)

        val crypto01 = Crypto.getInstance(app.preference)
        val str01 = "Hello World!"
        val enc01 = Crypto.encrypt(str01, crypto01.publicKey)
        val decStr01 = Crypto.decrypt(enc01, crypto01.privateKey)
        assertEquals(str01, decStr01)
    }
}