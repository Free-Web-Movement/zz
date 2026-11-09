package io.github.freewebmovement.android.noui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference

/** Handles control-bar actions that are not plain service start/stop. */
class ZzControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_TOGGLE_WEB) {
            val settings = KVSettings(Preference(Settings()))
            settings.network.staticFileEnabled = !settings.network.staticFileEnabled
            SystemStatusNotifier.refresh(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_WEB = "io.github.freewebmovement.fwmc.TOGGLE_WEB"
    }
}
