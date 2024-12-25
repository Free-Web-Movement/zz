package io.github.freewebmovement.zz

import android.app.Application
import android.content.Context

class MainApplication : Application() {

    init {
        instance = this
    }

    companion object {
        public var instance: MainApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
    }
}