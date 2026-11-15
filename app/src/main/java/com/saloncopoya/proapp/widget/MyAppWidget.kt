package com.saloncopoya.proapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Button // 🔥 IMPORTANTE: Importación correcta para el botón
import com.saloncopoya.proapp.MainActivity

class MyAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(100.dp, 40.dp), DpSize(100.dp, 100.dp), DpSize(200.dp, 100.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { MyContent() }
    }

    @Composable
    private fun MyContent() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color(0xFF6200EE)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Widget Pro", 
                style = TextStyle(color = ColorProvider(Color.White))
            )
            // 🔥 CORRECCIÓN: Eliminamos el parámetro 'style' que causaba el error.
            Button(
                text = "Abrir App",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.padding(top = 8.dp)
            )
        }
    }
}
