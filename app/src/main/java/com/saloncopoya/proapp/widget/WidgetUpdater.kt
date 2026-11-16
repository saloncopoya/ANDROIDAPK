package com.saloncopoya.proapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fuerza la actualización de TODOS los widgets de la app.
 */
fun forceWidgetUpdate(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try { MyAppWidget().updateAll(context) } catch (_: Exception) {}
        try { QuickActionWidget().updateAll(context) } catch (_: Exception) {}
        try { ListAppWidget().updateAll(context) } catch (_: Exception) {}
    }
}
