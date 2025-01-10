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


const val PREFERENCES_NAME = "ZZ"

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    lateinit var preference: SharedPreferences
    lateinit var crypto: Crypto
    lateinit var db : ZzDatabase
    lateinit var settings: Settings
    lateinit var ipList: IPList

    init {
        instance = this
        coroutineScope.launch {
            preference = applicationContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            crypto = Crypto.getInstance(preference)
            settings = Settings(preference)
            ipList = IPList.getInstance(settings)
            PeerServer.start(instance!!, Server.host, settings.localServerPort)
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