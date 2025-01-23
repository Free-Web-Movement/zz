package io.github.freewebmovement.peer

import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.system.crypto.Crypto
import kotlinx.coroutines.CoroutineScope
import java.io.File

// Only IP is Allowed
enum class IPType {
    IPV4,
    IPV6
}

enum class AddressScriptType {
    M2PK // Message to Public Key Script Type
}


interface IPreference {
    fun <T> save(key: String, value: T)
    fun <T> read(key: String, value: T) : T
}

interface IDownload {
    fun myApk(): File
    fun downloadDir(): File
}

interface IApp {
        val scope: CoroutineScope
        val crypto: Crypto
        fun setIpInfo(json: PublicKeyJSON)
        fun getDownloadDir(): File
        fun getProfile(): UserJSON
}