package io.github.freewebmovement.zz.net.api.crypto

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import io.github.freewebmovement.zz.persistence.Preference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


private val IS_INIT_KEY = booleanPreferencesKey("kp_is_init")
private val PRIVATE_KEY = byteArrayPreferencesKey("kp_private_key")
private val PUBLIC_KEY = byteArrayPreferencesKey("kp_public_key")
private const val CRYPTO_ALGORITHM_RSA = "RSA"
private const val KEY_SIZE = 2048

class Crypto(aPrivateKey: PrivateKey, aPublicKey: PublicKey) {
    // For public keys
    var privateKey: PrivateKey
    var publicKey: PublicKey
    init {
        privateKey = aPrivateKey
        publicKey = aPublicKey
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(str: String): String {
        val cipher = Cipher.getInstance(CRYPTO_ALGORITHM_RSA)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val strBytes = str.toByteArray(StandardCharsets.UTF_8)
        val encBytes = cipher.doFinal(strBytes)
        return Base64.encode(encBytes)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun dec(str: String): String {
        val cipher = Cipher.getInstance(CRYPTO_ALGORITHM_RSA)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val encBytes = Base64.decode(str)
        val strBytes = cipher.doFinal(encBytes)
        return strBytes.toString(StandardCharsets.UTF_8)
    }

    companion object {
        private val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(CRYPTO_ALGORITHM_RSA)
        private val keyFactory: KeyFactory = KeyFactory.getInstance(CRYPTO_ALGORITHM_RSA)
        private var instance: Crypto? = null
        fun getInstance(preference: Preference): Crypto {
            GlobalScope.launch {
                if (instance == null) {
                    val isInit = preference.read(IS_INIT_KEY)
                    if (isInit != true) {
                        kpg.initialize(KEY_SIZE)
                        preference.write(PRIVATE_KEY, kpg.genKeyPair().private.encoded)
                        preference.write(PUBLIC_KEY, kpg.genKeyPair().private.encoded)
                        instance = Crypto(kpg.genKeyPair().private, kpg.genKeyPair().public)
                    } else {
                        lateinit var privateKey: PrivateKey
                        lateinit var publicKey: PublicKey
                        val privateKeyBytes = preference.read(PRIVATE_KEY)!!
                        val publicKeyBytes = preference.read(PUBLIC_KEY)!!
                        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
                        privateKey = keyFactory.generatePrivate(privateKeySpec)
                        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
                        publicKey = keyFactory.generatePublic(publicKeySpec)
                        instance = Crypto(privateKey, publicKey)
                    }
                }
            }
            return instance!!
        }
    }
}