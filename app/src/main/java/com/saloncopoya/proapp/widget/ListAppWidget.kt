package com.saloncopoya.proapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.defaultWeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.saloncopoya.proapp.MainActivity
import com.saloncopoya.proapp.utils.SystemTools
import com.saloncopoya.proapp.widget.actions.IncrementAction
import com.saloncopoya.proapp.widget.actions.NotifyAction
import com.saloncopoya.proapp.widget.actions.ResetAction
import com.saloncopoya.proapp.widget.actions.VibrateAction

class ListAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val battery = SystemTools.batteryLevel(context)
        val network = SystemTools.networkType(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val counter = prefs[longPreferencesKey("counter")] ?: 0L
            val lastNotif = prefs[stringPreferencesKey("last_notif")] ?: "Nunca"
            WidgetContent(battery, network, counter, lastNotif)
        }
    }

    @Composable
    private fun WidgetContent(battery: Int, network: String, counter: Long, lastNotif: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(10.dp)
        ) {
            // Header
            Text(
                text = "🚀 App Pro • v2.0",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(6.dp))

            // Panel info
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(Color(0xFF16213E))
                    .padding(8.dp)
            ) {
                InfoRow("🔋 Batería", "$battery%")
                InfoRow("🌐 Red", network)
                InfoRow("🔢 Contador", counter.toString())
                InfoRow("🔔 Última notif", lastNotif)
            }

            Spacer(GlanceModifier.height(8.dp))

            // Fila 1: Abrir + Notif
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetButton(
                    text = "📱 Abrir",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionStartActivity<MainActivity>()
                )
                Spacer(GlanceModifier.width(4.dp))
                WidgetButton(
                    text = "🔔 Notif",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionRunCallback<NotifyAction>()
                )
            }

            Spacer(GlanceModifier.height(4.dp))

            // Fila 2: +1 / Vibrar / Reset
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetButton(
                    text = "➕ +1",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionRunCallback<IncrementAction>()
                )
                Spacer(GlanceModifier.width(4.dp))
                WidgetButton(
                    text = "📳 Vibrar",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionRunCallback<VibrateAction>()
                )
                Spacer(GlanceModifier.width(4.dp))
                WidgetButton(
                    text = "🔄 Reset",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionRunCallback<ResetAction>()
                )
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(Color(0xFFB0B0B0)))
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = value,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun WidgetButton(
        text: String,
        modifier: GlanceModifier = GlanceModifier,
        onClick: Action
    ) {
        androidx.glance.Button(
            text = text,
            onClick = onClick,
            modifier = modifier
        )
    }
}
