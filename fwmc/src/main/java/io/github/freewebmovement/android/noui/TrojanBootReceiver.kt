package io.github.freewebmovement.android.noui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference

/** Starts the Trojan VPN after boot when the user enabled auto-start. */
class TrojanBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val setting = KVSettings(Preference(Settings())).trojan
        if (!setting.autoStart || setting.currentServer() == null) return
        runCatching { TrojanService.start(context) }
    }
}
