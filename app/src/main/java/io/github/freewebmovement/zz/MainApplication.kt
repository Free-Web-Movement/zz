package io.github.freewebmovement.zz

import android.app.Application
import io.github.freewebmovement.peer.database.AppDatabase
import io.github.freewebmovement.system.Settings
import io.github.freewebmovement.zz.bussiness.Share
import io.github.freewebmovement.system.crypto.Crypto
import io.github.freewebmovement.zz.system.getDatabase
import io.github.freewebmovement.peer.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.peer.PeerClient
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.net.RoomHandler
import io.github.freewebmovement.zz.system.persistence.Preference
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val PREFERENCES_NAME = "ZZ"

class MainApplication : Application() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    lateinit var preference: Preference
    lateinit var crypto: Crypto
    lateinit var db : AppDatabase
    lateinit var settings: Settings
    lateinit var ipList: IPList
    lateinit var share: Share
    lateinit var handler: IInstrumentedHandler
    lateinit var peerClient: PeerClient
    lateinit var peerServer: PeerServer

    init {
        instance = this
        coroutineScope.launch {
            preference = Preference(applicationContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
            crypto = Crypto.getInstance(preference)
            settings = Settings(preference)
            ipList = IPList.getInstance(settings)
            share = Share(applicationContext)
            db = getDatabase(applicationContext)
            val client = HttpClient(CIO)
            handler = RoomHandler(instance!!)
            peerClient = PeerClient(client, handler)
            peerServer = PeerServer.start(instance!!, "0.0.0.0", settings.localServerPort)
        }
    }

    companion object {
        var instance: MainApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
    }
}