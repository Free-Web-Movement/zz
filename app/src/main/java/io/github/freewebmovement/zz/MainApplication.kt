package io.github.freewebmovement.zz

import android.app.Application
import android.content.Context
import io.github.freewebmovement.peer.PeerClient
import io.github.freewebmovement.peer.PeerServer
import io.github.freewebmovement.peer.database.entity.Peer
import io.github.freewebmovement.peer.realize.RoomHandler
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.peer.types.IPType
import io.github.freewebmovement.zz.bussiness.Share
import io.github.freewebmovement.zz.system.getDatabase
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val PREFERENCES_NAME = "ZZ"

class MainApplication : Application() {
    lateinit var app: MyApp

    companion object {
        var instance: MainApplication? = null

        fun getApp(): MyApp {
            return instance!!.app
        }
    }

    override fun onCreate() {
        instance = this
        app = MyApp(this)
        app.scope = CoroutineScope(Dispatchers.IO)
        app.scope.launch {
            val settings = Settings()
            app.preference = Preference(settings)
            app.crypto = Crypto.getInstance(app.preference)
            app.settings = KVSettings(app.preference)
            app.share = Share(applicationContext)
            app.db = getDatabase(applicationContext)
            app.handler = RoomHandler(app)
            app.client = PeerClient( app.handler)
            val peer = Peer("0.0.0.0", app.settings.network.localServerPort, IPType.IPV4)
            app.server = PeerServer(app, peer)
        }
        super.onCreate()
    }
}