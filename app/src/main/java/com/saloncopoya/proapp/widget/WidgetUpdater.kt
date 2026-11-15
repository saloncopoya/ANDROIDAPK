package com.saloncopoya.proapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Esta función fuerza a Glance a redibujar el widget con el contenido actual
fun forceWidgetUpdate(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        MyAppWidget().updateAll(context)
    }
}
