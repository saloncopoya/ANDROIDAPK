package com.saloncopoya.proapp.widget.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import com.saloncopoya.proapp.widget.ListAppWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifyAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val channelId = "canal_general"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(channelId, "General", NotificationManager.IMPORTANCE_HIGH))
        }
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Acción desde el Widget! 🚀")
            .setContentText("Enviado a las $now")
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        context.getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % 100000).toInt(), notif)

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[stringPreferencesKey("last_notif")] = now
            }
        }
        ListAppWidget().updateAll(context)
    }
}

class VibrateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(300)
        }
    }
}

class IncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                val key = longPreferencesKey("counter")
                this[key] = (this[key] ?: 0L) + 1L
            }
        }
        ListAppWidget().updateAll(context)
    }
}

class ResetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[longPreferencesKey("counter")] = 0L
            }
        }
        ListAppWidget().updateAll(context)
    }
}
