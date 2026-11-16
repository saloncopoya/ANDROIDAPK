package com.saloncopoya.proapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.saloncopoya.proapp.MainActivity
import com.saloncopoya.proapp.R
import com.saloncopoya.proapp.utils.SystemTools

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private var collapsedRoot: View? = null
    private var expandedRoot: View? = null
    private var currentScale = 1.0f
    private var actionCount = 0

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val CHANNEL_ID = "floating_channel"
    private val NOTIF_ID = 4242

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startFg()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloating()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Ventana Flotante", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun startFg() {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Ventana Flotante Activa")
            .setContentText("Toca para abrir la app. Arrastra la ventanita.")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun showFloating() {
        val themed = ContextThemeWrapper(this, R.style.Theme_AppNativaPro)
        floatingView = LayoutInflater.from(themed).inflate(R.layout.floating_window, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 200
        }

        bindViews()
        windowManager.addView(floatingView, params)
    }

    private fun bindViews() {
        collapsedRoot = floatingView.findViewById(R.id.collapsedRoot)
        expandedRoot = floatingView.findViewById(R.id.expandedRoot)

        // Burbuja colapsada
        val bubble = floatingView.findViewById<View>(R.id.collapsedBubble)
        bubble.setOnTouchListener(dragListener)
        bubble.setOnClickListener {
            setExpanded(true)
        }

        // Header arrastrable
        floatingView.findViewById<View>(R.id.dragHandle).setOnTouchListener(dragListener)

        // Botones de control
        floatingView.findViewById<View>(R.id.btnCollapse).setOnClickListener { setExpanded(false) }
        floatingView.findViewById<View>(R.id.btnClose).setOnClickListener { stopSelf() }

        // Botón abrir app
        floatingView.findViewById<View>(R.id.btnOpenApp).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
        }

        // Notificación
        floatingView.findViewById<View>(R.id.btnNotif).setOnClickListener {
            sendQuickNotification()
            incrementCounter()
        }

        // Vibrar
        floatingView.findViewById<View>(R.id.btnVibrate).setOnClickListener {
            vibrate()
            incrementCounter()
        }

        // Cambiar tamaño
        floatingView.findViewById<View>(R.id.btnSize).setOnClickListener {
            currentScale = if (currentScale == 1.0f) 1.35f else 1.0f
            floatingView.scaleX = currentScale
            floatingView.scaleY = currentScale
            Toast.makeText(this, "Tamaño: ${(currentScale * 100).toInt()}%", Toast.LENGTH_SHORT).show()
        }

        // Toast
        floatingView.findViewById<View>(R.id.btnToast).setOnClickListener {
            Toast.makeText(this, "¡Hola desde la ventana flotante! 👋", Toast.LENGTH_SHORT).show()
            incrementCounter()
        }

        // PiP
        floatingView.findViewById<View>(R.id.btnPipFloat).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("enter_pip", true)
            })
        }

        // 🔦 Linterna (NUEVO)
        floatingView.findViewById<View>(R.id.btnFlash).setOnClickListener {
            val on = SystemTools.toggleFlashlight(this)
            Toast.makeText(this, if (on) "🔦 Linterna ON" else "🔦 Linterna OFF", Toast.LENGTH_SHORT).show()
            incrementCounter()
        }

        // 🔊 TTS (NUEVO)
        floatingView.findViewById<View>(R.id.btnSpeak).setOnClickListener {
            SystemTools.speak(this, "Hola, soy la ventana flotante de App Pro")
            incrementCounter()
        }

        // 🎨 QR (NUEVO)
        floatingView.findViewById<View>(R.id.btnQrFloat).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("accion", "qr")
            })
        }
    }

    private fun incrementCounter() {
        actionCount++
        floatingView.findViewById<TextView>(R.id.tvCounter)?.text = "Acciones: $actionCount"
    }

    private fun setExpanded(expand: Boolean) {
        collapsedRoot?.visibility = if (expand) View.GONE else View.VISIBLE
        expandedRoot?.visibility = if (expand) View.VISIBLE else View.GONE
    }

    private fun sendQuickNotification() {
        val mgr = getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Acción desde ventana flotante")
            .setContentText("¡Funciona sin abrir la app principal!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        mgr.notify((System.currentTimeMillis() % 10000).toInt(), notif)
        Toast.makeText(this, "🔔 Notificación enviada", Toast.LENGTH_SHORT).show()
    }

    private fun vibrate() {
        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(350)
        }
        Toast.makeText(this, "📳 Vibrando", Toast.LENGTH_SHORT).show()
    }

    private val dragListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (kotlin.math.abs(dx) > 10f || kotlin.math.abs(dy) > 10f)) {
                    isDragging = true
                }
                if (isDragging) {
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    try { windowManager.updateViewLayout(floatingView, params) } catch (_: Exception) {}
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) view.performClick()
                isDragging = false
                true
            }
            else -> true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            try { windowManager.removeView(floatingView) } catch (_: Exception) {}
        }
    }
}
