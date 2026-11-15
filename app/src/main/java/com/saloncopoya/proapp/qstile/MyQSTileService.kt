package com.saloncopoya.proapp.qstile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.saloncopoya.proapp.MainActivity

class MyQSTileService : TileService() {
    private var isActive = false

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        isActive = !isActive
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "App Pro: ON" else "App Pro: OFF"
        tile.updateTile()

        if (isActive) {
            sendNotificationFromTile()
        } else {
            // Si lo apagas, podrías cancelar algo
        }
    }

    private fun sendNotificationFromTile() {
        val channelId = "canal_general"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(channelId, "General", NotificationManager.IMPORTANCE_HIGH))
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Acción desde Ajustes Rápidos!")
            .setContentText("Tocaste el Tile sin abrir la app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            
        getSystemService(NotificationManager::class.java).notify(99, notification)
    }
}
