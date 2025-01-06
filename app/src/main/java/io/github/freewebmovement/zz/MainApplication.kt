package io.github.freewebmovement.zz

import android.app.Application
import android.content.SharedPreferences
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.settings.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private const val PREFERENCES_NAME = "ZZ"

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    lateinit var preference: SharedPreferences
    lateinit var crypto: Crypto
    lateinit var db : ZzDatabase
    var ipList: IPList = IPList.getInstance(Server.port)
    lateinit var settings: Settings

    init {
        instance = this
        coroutineScope.launch {
            preference = applicationContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            crypto = Crypto.getInstance(preference)
            settings = Settings(preference)
//            PeerServer.start(Server.host, settings.localServerPort)
            PeerServer.start(Server.host, Server.port)
            db = ZzDatabase.getDatabase(applicationContext)
        }
    }

    companion object {
        var instance: MainApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
    }
}