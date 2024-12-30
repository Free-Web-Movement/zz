package io.github.freewebmovement.zz

import android.app.Application
import io.github.freewebmovement.zz.system.database.ZzDatabase
import io.github.freewebmovement.zz.system.net.IPList
import io.github.freewebmovement.zz.system.net.PeerServer
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.persistence.Preference
import io.github.freewebmovement.zz.system.settings.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var preference: Preference
    lateinit var crypto: Crypto
    lateinit var db : ZzDatabase
    lateinit var ipList: IPList

    init {
        instance = this
        coroutineScope.launch {
            preference = Preference(applicationContext)
            crypto = Crypto.getInstance(preference)
            PeerServer.start(Server.host, Server.port)
            db = ZzDatabase.getDatabase(applicationContext)
            ipList = IPList.getInstance(Server.port)
        }
    }

    companion object {
        var instance: MainApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
    }
}