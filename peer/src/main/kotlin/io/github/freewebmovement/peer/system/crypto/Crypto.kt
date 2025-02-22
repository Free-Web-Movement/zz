package io.github.freewebmovement.peer.system.crypto

import io.github.freewebmovement.peer.types.AddressScriptType
import io.github.freewebmovement.peer.interfaces.IPreference
import io.github.freewebmovement.peer.json.SignJSON
import io.ktor.util.hex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
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
private const val CRYPTO_ALGORITHM_SHA256 = "SHA-256"
private const val CRYPTO_ALGORITHM_SHA256_WITH_RSA = "SHA256withRSA"

private const val KEY_SIZE = 2048

const val M2PK_PREFIX_VERSION = "VIG0"

class Crypto(aPrivateKey: PrivateKey, aPublicKey: PublicKey) {
    // For public keys
    var privateKey: PrivateKey = aPrivateKey
    var publicKey: PublicKey = aPublicKey

    companion object {
        private val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(CRYPTO_ALGORITHM_RSA)
        private val keyFactory: KeyFactory = KeyFactory.getInstance(CRYPTO_ALGORITHM_RSA)

        private lateinit var preference: IPreference
        fun refresh(preference: IPreference): Crypto {
            preference.save(IS_INIT_KEY, false)
            return getInstance(preference)
        }

        fun sign(message: ByteArray, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance(CRYPTO_ALGORITHM_SHA256_WITH_RSA)
            signature.initSign(privateKey)
            signature.update(message)
            return signature.sign()
        }

        fun verify(message: ByteArray, sign: ByteArray, publicKey: PublicKey): Boolean {
            val signature = Signature.getInstance(CRYPTO_ALGORITHM_SHA256_WITH_RSA)
            signature.initVerify(publicKey)
            signature.update(message)
            return signature.verify(sign)
        }

        fun toAddress(
            publicKey: PublicKey,
            // reserved for further scripts matching new address formats
            addressType: AddressScriptType = AddressScriptType.M2PK
        ): String {
            return when (addressType) {
                AddressScriptType.M2PK -> M2PK_PREFIX_VERSION + toHash160(publicKey)
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun toHash160(publicKey: PublicKey): String {
            Security.addProvider(BouncyCastleProvider())
            val sha256 = MessageDigest
                .getInstance(CRYPTO_ALGORITHM_SHA256)
                .digest(publicKey.encoded)

            val d = RIPEMD160Digest()
            d.update(sha256, 0, sha256.size)
            val o = ByteArray(d.digestSize)
            d.doFinal(o, 0)
            return o.toHexString()
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

        fun saveKey(preference: IPreference, key: String, value: ByteArray) {
            preference.save(key, hex(value))
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun readKey(preference: IPreference, key: String): ByteArray {
            return preference.read(key, "").hexToByteArray()
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun fromHexString(key: String): PublicKey {
            return revokePublicKey(key.hexToByteArray())
        }

        fun toHexString(key: PublicKey): String {
            return hex(key.encoded)
        }

        fun revokePublicKey(publicKeyBytes: ByteArray): PublicKey {
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
            return keyFactory.generatePublic(publicKeySpec)
        }

        fun revokePrivateKey(privateKeyBytes: ByteArray): PrivateKey {
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            return keyFactory.generatePrivate(privateKeySpec)
        }

        fun createCrypto(): Crypto {
            val keyPair = kpg.genKeyPair()
            return Crypto(keyPair.private, keyPair.public)
        }

        fun getInstance(prefer: IPreference): Crypto {
            preference = prefer
            val isInit: Boolean = prefer.read(IS_INIT_KEY, false)
            val instance: Crypto
            if (!isInit) {
                kpg.initialize(KEY_SIZE)
                val keyPair = kpg.genKeyPair()
                saveKey(prefer, PRIVATE_KEY, keyPair.private.encoded)
                saveKey(prefer, PUBLIC_KEY, keyPair.public.encoded)
                instance = Crypto(keyPair.private, keyPair.public)
                prefer.save(IS_INIT_KEY, true)
            } else {
                val privateKeyBytes = readKey(prefer, PRIVATE_KEY)
                val publicKeyBytes = readKey(prefer, PUBLIC_KEY)
                instance = Crypto(
                    revokePrivateKey(privateKeyBytes),
                    revokePublicKey(publicKeyBytes)
                )
            }
            return instance
        }

        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> sign(json: T, crypto: Crypto): String {
            val jsonStr = Json.encodeToString(json)
            val sign = sign(jsonStr.toByteArray(), crypto.privateKey).toHexString()
            return Json.encodeToString(SignJSON(jsonStr, sign, toHexString(crypto.publicKey)))
        }

        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> verify(json: String): T {
            val signJSON = Json.decodeFromString<SignJSON>(json)
            val resJson = Json.decodeFromString<T>(signJSON.json)
            assert(
                verify(
                    signJSON.json.toByteArray(),
                    signJSON.signature.hexToByteArray(),
                    fromHexString(signJSON.publicKey)
                )
            )
            return resJson
        }
    }
}