package io.github.freewebmovement.zz

import android.app.Application
import io.github.freewebmovement.android.noui.MyApp
import java.io.File

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
        app = MyApp.new(this)
        io.github.freewebmovement.android.noui.SeedList.ensure(this, File(app.fwmcRootDir()))
        rs.zz.coin.FwmcApi.initDataDir(app.fwmcRootDir())
        if (!io.github.freewebmovement.android.noui.TrojanService.running) {
            io.github.freewebmovement.android.noui.SystemStatusNotifier.showIdle(this)
        }
        super.onCreate()
    }

    override fun onTerminate() {
        super.onTerminate()
        app.destroy()
    }
}