package io.github.freewebmovement.android.noui

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile for the fwmc base node (Zz). */
class ZzTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (SystemStatusNotifier.isNodeRunning()) {
            FwmcService.stop(this)
        } else {
            FwmcService.start(this)
        }
        Handler(Looper.getMainLooper()).postDelayed({ updateTile() }, 600)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = SystemStatusNotifier.isNodeRunning()
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Zz"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "节点 ${SystemStatusNotifier.nodeStatusValue()} · Epoch ${
                SystemStatusNotifier.epochValue()
            }"
        }
        tile.updateTile()
    }
}
