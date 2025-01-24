package io.github.freewebmovement.zz

import android.app.Application
import android.content.Context
import io.github.freewebmovement.peer.PeerClient
import io.github.freewebmovement.peer.PeerServer
import io.github.freewebmovement.peer.system.Settings
import io.github.freewebmovement.peer.system.crypto.Crypto
import io.github.freewebmovement.zz.bussiness.Share
import io.github.freewebmovement.zz.system.getDatabase
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.peer.RoomHandler
import io.github.freewebmovement.zz.system.persistence.Preference
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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

        fun getPreference(context: Context): Preference {
            return Preference(context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
        }
    }

    override fun onCreate() {
        instance = this
        app = MyApp(this)
        app.scope = CoroutineScope(Dispatchers.IO)
        app.scope.launch {
            app.preference = getPreference(applicationContext)
            app.crypto = Crypto.getInstance(app.preference)
            app.settings = Settings(app.preference)
            app.ipList = IPList.getInstance(app.settings)
            app.share = Share(applicationContext)
            app.db = getDatabase(applicationContext)
            app.handler = RoomHandler(app)
            val client = HttpClient(CIO)
            app.client = PeerClient(client, app.handler)
            app.server = PeerServer.start(app, "0.0.0.0", app.settings.localServerPort)
        }
        super.onCreate()
    }
}