package com.spamsentinel.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.spamsentinel.app.SpamSentinelApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProtectionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? SpamSentinelApp ?: return
        val prefs = app.container.preferences

        CoroutineScope(Dispatchers.IO).launch {
            val currentActive = prefs.protectionActive.first()
            prefs.setProtectionActive(!currentActive)
            updateTileState()
        }
    }

    private fun updateTileState() {
        val app = applicationContext as? SpamSentinelApp ?: return
        val prefs = app.container.preferences

        CoroutineScope(Dispatchers.Main).launch {
            val active = prefs.protectionActive.first()
            val tile = qsTile ?: return@launch

            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "SpamSentinel"
            tile.subtitle = if (active) "Active" else "Paused"
            tile.updateTile()
        }
    }
}
