package io.github.freewebmovement.android.noui

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference

/** Quick Settings tile that toggles the Trojan VPN service. */
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
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (TrojanService.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Trojan"
        tile.updateTile()
    }
}
