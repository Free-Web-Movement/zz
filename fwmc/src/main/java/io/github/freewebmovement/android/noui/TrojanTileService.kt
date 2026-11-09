package io.github.freewebmovement.android.noui

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference

/** Quick Settings tile for the Trojan VPN (value-added service). */
class TrojanTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (TrojanService.running) {
            TrojanService.stop(this)
        } else {
            val setting = KVSettings(Preference(Settings())).trojan
            if (setting.currentServer() != null) {
                TrojanService.start(this)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ updateTile() }, 600)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = TrojanService.running
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Trojan"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Trojan ${SystemStatusNotifier.trojanStatusValue()}"
        }
        tile.updateTile()
    }
}
