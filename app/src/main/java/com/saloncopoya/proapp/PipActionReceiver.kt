package com.saloncopoya.proapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PipActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_PLAY" -> Toast.makeText(context, "▶ Play presionado en PiP", Toast.LENGTH_SHORT).show()
            "ACTION_CLOSE" -> Toast.makeText(context, "❌ Cerrar presionado en PiP", Toast.LENGTH_SHORT).show()
        }
    }
}
