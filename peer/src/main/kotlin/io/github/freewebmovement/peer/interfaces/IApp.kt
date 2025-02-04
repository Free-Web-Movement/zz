package io.github.freewebmovement.peer.interfaces

import io.github.freewebmovement.peer.PeerClient
import io.github.freewebmovement.peer.PeerServer
import io.github.freewebmovement.peer.database.AppDatabase
import io.github.freewebmovement.peer.database.entity.AccountPeer
import io.github.freewebmovement.peer.json.PublicKeyJSON
import io.github.freewebmovement.peer.json.UserJSON
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.crypto.Crypto
import kotlinx.coroutines.CoroutineScope
import java.io.File

interface IApp {
    var preference: IPreference
    var scope: CoroutineScope
    var share: IShare
    var crypto: Crypto
    var handler: IInstrumentedHandler
    var client: PeerClient
    var server: PeerServer
    var db: AppDatabase
    var settings: KVSettings
    fun setIpInfo(json: PublicKeyJSON)
    fun getDownloadDir(): File
    fun getProfile(): UserJSON
    suspend fun getPeers(): List<AccountPeer>
}