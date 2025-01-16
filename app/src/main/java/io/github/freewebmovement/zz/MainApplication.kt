package io.github.freewebmovement.zz

import android.app.Application
import android.content.SharedPreferences
import io.github.freewebmovement.zz.bussiness.Settings
import io.github.freewebmovement.zz.bussiness.Share
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.crypto.Crypto
import io.github.freewebmovement.zz.system.persistence.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val PREFERENCES_NAME = "ZZ"

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    lateinit var preference: Preference
    lateinit var crypto: Crypto
    lateinit var db : ZzDatabase
    lateinit var settings: Settings
    lateinit var ipList: IPList
    lateinit var share: Share

    init {
        instance = this
        coroutineScope.launch {
            preference = Preference(applicationContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
            crypto = Crypto.getInstance(preference)
            settings = Settings(preference)
            ipList = IPList.getInstance(settings)
            share = Share(applicationContext)
            PeerServer.start(instance!!, "0.0.0.0", settings.localServerPort)
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