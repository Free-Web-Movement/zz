package io.github.freewebmovement.zz.system.net.api.crypto

import android.content.SharedPreferences
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


private const val IS_INIT_KEY = "kp_is_init"
private const val PRIVATE_KEY = "kp_private_key"
private const val PUBLIC_KEY = "kp_public_key"
private const val CRYPTO_ALGORITHM_RSA = "RSA"
private const val KEY_SIZE = 2048

class Crypto(aPrivateKey: PrivateKey, aPublicKey: PublicKey) {
    // For public keys
    var privateKey: PrivateKey = aPrivateKey
    var publicKey: PublicKey = aPublicKey
    
    companion object {
        private val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(CRYPTO_ALGORITHM_RSA)
        private val keyFactory: KeyFactory = KeyFactory.getInstance(CRYPTO_ALGORITHM_RSA)
        private var instance: Crypto? = null
        fun refresh(preference: SharedPreferences) : Crypto {
            instance = null
            val editor = preference.edit()
            editor.putBoolean(IS_INIT_KEY, false)
            editor.apply()
            return getInstance(preference)
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun encrypt(str: String, key: PublicKey): String {
            val cipher = Cipher.getInstance(CRYPTO_ALGORITHM_RSA)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val strBytes = str.toByteArray(StandardCharsets.UTF_8)
            val encBytes = cipher.doFinal(strBytes)
            return Base64.encode(encBytes)
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun decrypt(str: String, key: PrivateKey): String {
            val cipher = Cipher.getInstance(CRYPTO_ALGORITHM_RSA)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val encBytes = Base64.decode(str)
            val strBytes = cipher.doFinal(encBytes)
            return strBytes.toString(StandardCharsets.UTF_8)
        }

        fun revokePublicKey(publicKeyBytes: ByteArray): PublicKey {
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
            return keyFactory.generatePublic(publicKeySpec)
        }

        fun revokePrivateKey(privateKeyBytes: ByteArray): PrivateKey {
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            return keyFactory.generatePrivate(privateKeySpec)
        }
        fun getInstance(preference: SharedPreferences): Crypto {
                if (instance == null) {
                    val isInit = preference.getBoolean(IS_INIT_KEY, false)
                    if (!isInit) {
                        kpg.initialize(KEY_SIZE)
                        val editor = preference.edit()
                        editor.putString(PRIVATE_KEY, kpg.genKeyPair().private.encoded.toString())
                        editor.putString(PUBLIC_KEY, kpg.genKeyPair().public.encoded.toString())
                        editor.apply()
                        instance = Crypto(kpg.genKeyPair().private, kpg.genKeyPair().public)
                    } else {
                        val privateKeyBytes = preference.getString(PRIVATE_KEY, "")?.toByteArray()
                        val publicKeyBytes = preference.getString(PUBLIC_KEY, "")?.toByteArray()
                        instance = Crypto(revokePrivateKey(privateKeyBytes!!),
                            revokePublicKey(publicKeyBytes!!)
                        )
                    }
                }
            return instance!!
        }
    }
}