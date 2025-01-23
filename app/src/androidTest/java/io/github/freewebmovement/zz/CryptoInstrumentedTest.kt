package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.system.crypto.M2PK_PREFIX_VERSION
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoInstrumentedTest {
    @Test
    fun should_test_key_pair() = runTest {
        val context = ApplicationProvider.getApplicationContext<MainApplication>()
        val preference = MainApplication.getPreference(context)
        val crypto = Crypto.getInstance(preference)
        val str = "Hello World!"
        val enc = Crypto.encrypt(str, crypto.publicKey)
        val decStr = Crypto.decrypt(enc, crypto.privateKey)
        assertEquals(str, decStr)
        val sign = Crypto.sign(str.toByteArray(), crypto.privateKey);
        val verify = Crypto.verify(str.toByteArray(), sign, crypto.publicKey);

        assert(verify)

        Crypto.refresh(preference)

        val crypto01 = Crypto.getInstance(preference)
        val str01 = "Hello World!"
        val enc01 = Crypto.encrypt(str01, crypto01.publicKey)
        val decStr01 = Crypto.decrypt(enc01, crypto01.privateKey)
        assertEquals(str01, decStr01)

        val publicKeyKey = "PUBLIC_KEY"
        val privateKeyKey = "PRIVATE_KEY"
        Crypto.saveKey(preference, publicKeyKey, crypto.publicKey.encoded)
        val publicEncoded = Crypto.readKey(preference, publicKeyKey)
        assert(publicEncoded.contentEquals(crypto.publicKey.encoded))

        Crypto.saveKey(preference, privateKeyKey, crypto.privateKey.encoded)
        val privateEncoded = Crypto.readKey(preference, privateKeyKey)
        assert(privateEncoded.contentEquals(crypto.privateKey.encoded))

        val revokedPrivate = Crypto.revokePrivateKey(privateEncoded)
        val revokedPublic = Crypto.revokePublicKey(publicEncoded)

        assert(revokedPublic == crypto.publicKey)
        assert(revokedPrivate == crypto.privateKey)

        val address01 = Crypto.toAddress(crypto.publicKey)
        val address02 = Crypto.toAddress(crypto.publicKey)

        assert(address01.substring(0, 4) == M2PK_PREFIX_VERSION)
        assert(address02 == address01)
    }
}