package io.github.freewebmovement.zz.net.api.crypto

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import io.github.freewebmovement.zz.persistence.Preference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.KeyPairGenerator
private val IS_INIT_KEY = booleanPreferencesKey("kp_is_init")
private val PRIVATE_KEY = byteArrayPreferencesKey("kp_private_key")
private val PUBLIC_KEY = byteArrayPreferencesKey("kp_public_key")
private const val CRYPTO_RSA = "RSA"
private const val KEY_SIZE = 2048

@Suppress("OPT_IN_USAGE")
class KeyPair(preference: Preference) {
    lateinit var privateKey:ByteArray
    lateinit var  publicKey:ByteArray
    init {
        GlobalScope.launch {
            val isInit = preference.read(IS_INIT_KEY)
            if (isInit != true) {
                val kpg = KeyPairGenerator.getInstance(CRYPTO_RSA)
                kpg.initialize(KEY_SIZE)
                val kp = kpg.genKeyPair()
                privateKey = kp.private.encoded
                publicKey = kp.public.encoded
                preference.write(PRIVATE_KEY, privateKey)
                preference.write(PUBLIC_KEY, publicKey)
            } else {
                privateKey = preference.read(PRIVATE_KEY)!!
                publicKey = preference.read(PUBLIC_KEY)!!
            }
        }
    }
}