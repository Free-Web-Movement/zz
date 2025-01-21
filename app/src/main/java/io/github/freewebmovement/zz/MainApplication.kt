package io.github.freewebmovement.zz

import android.app.Application
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.bussiness.Share
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.system.net.PeerClient
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.net.api.IInstrumentedHandler
import io.github.freewebmovement.zz.system.net.api.RoomHandler
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
    lateinit var db : ZzDatabase
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
            db = ZzDatabase.getDatabase(applicationContext)
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