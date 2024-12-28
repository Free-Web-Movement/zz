package io.github.freewebmovement.zz

import android.app.Application
import android.provider.Settings.Global
import io.github.freewebmovement.zz.net.HttpServer
import io.github.freewebmovement.zz.net.api.crypto.Crypto
import io.github.freewebmovement.zz.persistence.Preference
import io.github.freewebmovement.zz.settings.Server
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application() {
    private lateinit var preference:Preference
    lateinit var crypto:Crypto

    init {
        instance = this
        GlobalScope.launch {
            preference = Preference(applicationContext)
            crypto = Crypto.getInstance(preference)
            HttpServer.start(Server.host, Server.port)
        }
    }

    companion object {
        var instance: MainApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
    }
}