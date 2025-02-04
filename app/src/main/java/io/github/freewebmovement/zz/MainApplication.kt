package io.github.freewebmovement.zz
import android.app.Application
import io.github.freewebmovement.android.noui.MyApp

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
        super.onCreate()
    }
}