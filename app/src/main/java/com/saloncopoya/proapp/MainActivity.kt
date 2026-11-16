package com.saloncopoya.proapp

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.material.snackbar.Snackbar
import com.saloncopoya.proapp.databinding.ActivityMainBinding
import com.saloncopoya.proapp.service.FloatingWindowService
import com.saloncopoya.proapp.service.MyForegroundService
import com.saloncopoya.proapp.utils.SystemTools
import com.saloncopoya.proapp.widget.forceWidgetUpdate
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private val OVERLAY_REQ = 501
    private val WRITE_SET_REQ = 502
    private var counter = 0
    private var sensorManager: SensorManager? = null
    private var accelValues = "—"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { !it }) {
            Toast.makeText(this, "⚠️ Algunos permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        forceWidgetUpdate(this)
        askPermissions()
        setupButtons()
        createNotificationChannels()
        handleDeepLink(intent)
        loadCounter()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (intent.getBooleanExtra("enter_pip", false)) {
            binding.root.postDelayed({ enterPipMode() }, 400)
        }
        handleShortcutExtras(intent)
    }

    private fun handleShortcutExtras(intent: Intent?) {
        when (intent?.getStringExtra("accion")) {
            "notificar" -> showSimpleNotification()
            "flotante" -> startFloatingWindow()
            "qr" -> generateAndShowQr()
            "sistema" -> showSystemAll()
        }
    }

    private fun loadCounter() {
        counter = getSharedPreferences("app_pro", MODE_PRIVATE).getInt("counter", 0)
    }

    private fun saveCounter() {
        getSharedPreferences("app_pro", MODE_PRIVATE).edit().putInt("counter", counter).apply()
    }

    private fun askPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun setupButtons() {
        with(binding) {
            // Flotante
            btnFloatingStart.setOnClickListener { startFloatingWindow() }
            btnFloatingStop.setOnClickListener { stopFloatingWindow() }
            // Notif
            btnNotifSimple.setOnClickListener { showSimpleNotification() }
            btnNotifImage.setOnClickListener { showImageNotification() }
            btnNotifActions.setOnClickListener { showActionNotification() }
            btnNotifVibrate.setOnClickListener { showVibrateNotification() }
            btnClearNotifs.setOnClickListener { clearAllNotifications() }
            // Productividad
            btnCopyClip.setOnClickListener {
                SystemTools.copyToClipboard(this@MainActivity, "Hola desde AppNativaPro 🚀")
            }
            btnPasteClip.setOnClickListener { SystemTools.pasteFromClipboard(this@MainActivity) }
            btnTts.setOnClickListener {
                SystemTools.speak(this@MainActivity, "Hola, esta es la app nativa pro. Estoy leyendo este texto en voz alta.")
            }
            btnCounter.setOnClickListener {
                counter++; saveCounter()
                toast("➕ Contador: $counter")
                forceWidgetUpdate(this@MainActivity)
            }
            btnCounterReset.setOnClickListener {
                counter = 0; saveCounter()
                toast("🔄 Contador reseteado")
                forceWidgetUpdate(this@MainActivity)
            }
            // Hardware
            btnFlashlight.setOnClickListener {
                val on = SystemTools.toggleFlashlight(this@MainActivity)
                toast(if (on) "🔦 Linterna ON" else "🔦 Linterna OFF")
            }
            btnVolUp.setOnClickListener { toast("🔊 Volumen: ${SystemTools.adjustVolume(this@MainActivity, true)}%") }
            btnVolDown.setOnClickListener { toast("🔉 Volumen: ${SystemTools.adjustVolume(this@MainActivity, false)}%") }
            btnBrightness.setOnClickListener { changeBrightness() }
            btnFancyVibrate.setOnClickListener { SystemTools.fancyVibrate(this@MainActivity) }
            // Info
            btnBattery.setOnClickListener {
                showDialog("🔋 Batería", SystemTools.batteryStatusText(this@MainActivity))
            }
            btnNetwork.setOnClickListener {
                showDialog("🌐 Red", SystemTools.networkDetails(this@MainActivity))
            }
            btnRam.setOnClickListener { showDialog("💾 RAM", SystemTools.ramInfo()) }
            btnDevice.setOnClickListener { showDialog("📱 Dispositivo", SystemTools.deviceInfo()) }
            btnSystemAll.setOnClickListener { showSystemAll() }
            // Personalización
            btnQr.setOnClickListener { generateAndShowQr() }
            btnWallpaper.setOnClickListener { setWallpaperDemo() }
            btnDarkMode.setOnClickListener { toggleDarkMode() }
            // Extras
            btnOpenUrl.setOnClickListener { SystemTools.openUrl(this@MainActivity, "https://saloncopoya.com") }
            btnEmail.setOnClickListener {
                SystemTools.sendEmail(this@MainActivity, "contacto@saloncopoya.com", "Hola desde App Pro")
            }
            btnDial.setOnClickListener { SystemTools.dialNumber(this@MainActivity, "+1234567890") }
            btnDialog.setOnClickListener { showDialog("💬 Diálogo", "Este es un diálogo de prueba generado desde código.") }
            btnSnackbar.setOnClickListener {
                Snackbar.make(binding.root, "📢 Snackbar de prueba", Snackbar.LENGTH_LONG)
                    .setAction("OK") { toast("Snackbar OK") }.show()
            }
            btnSensor.setOnClickListener { readSensors() }
            btnSound.setOnClickListener { SystemTools.playClickSound(this@MainActivity) }
            // Sistema
            btnStartService.setOnClickListener {
                ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, MyForegroundService::class.java))
                toast("▶️ Servicio iniciado")
            }
            btnPip.setOnClickListener { enterPipMode() }
            btnWorkManager.setOnClickListener { schedulePeriodicWork() }
            btnScan.setOnClickListener { toast("📷 Base CameraX lista (integra ScanActivity)") }
            btnShareText.setOnClickListener { shareText("¡Mira esta app profesional!") }
            btnShareImage.setOnClickListener { shareImage() }
            btnSettings.setOnClickListener { showSettingsDialog() }
            btnTestAll.setOnClickListener { runAllFeatures() }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  VENTANA FLOTANTE
    // ══════════════════════════════════════════════════════════════
    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

    private fun startFloatingWindow() {
        if (!canDrawOverlays()) {
            toast(getString(R.string.msg_overlay_needed))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                    OVERLAY_REQ
                )
            }
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, FloatingWindowService::class.java))
        toast(getString(R.string.msg_floating_started))
        binding.root.postDelayed({ moveTaskToBack(true) }, 500)
    }

    private fun stopFloatingWindow() {
        stopService(Intent(this, FloatingWindowService::class.java))
        toast(getString(R.string.msg_floating_stopped))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQ && canDrawOverlays()) startFloatingWindow()
        if (requestCode == WRITE_SET_REQ) changeBrightness()
    }

    // ══════════════════════════════════════════════════════════════
    //  NUEVAS FUNCIONES
    // ══════════════════════════════════════════════════════════════
    private fun changeBrightness() {
        if (!SystemTools.hasWriteSettings(this)) {
            toast("⚙️ Concede permiso de \"Modificar ajustes del sistema\"")
            startActivityForResult(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")), WRITE_SET_REQ)
            return
        }
        val current = SystemTools.getBrightness(this)
        val next = if (current >= 200) 50 else current + 80
        SystemTools.setBrightness(this, next)
        toast("☀️ Brillo: ${(next * 100) / 255}%")
    }

    private fun generateAndShowQr() {
        val bmp = SystemTools.generateQr("https://saloncopoya.com/proapp/abrir", 600) ?: run {
            toast("❌ Error generando QR"); return
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        container.addView(ImageView(this).apply {
            setImageBitmap(bmp)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 700)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })
        container.addView(TextView(this).apply {
            text = "https://saloncopoya.com/proapp/abrir"
            textSize = 11f
            setPadding(0, 16, 0, 0)
        })
        AlertDialog.Builder(this)
            .setTitle("🎨 Código QR generado")
            .setView(container)
            .setPositiveButton("Compartir") { _, _ -> shareBitmap(bmp) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun shareBitmap(bmp: Bitmap) {
        val uri = Uri.parse("android.resource://$packageName/${android.R.drawable.ic_menu_camera}")
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/*"
        }, "Compartir QR"))
    }

    private fun setWallpaperDemo() {
        try {
            val bmp = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawColor(0xFF6200EE.toInt())
            val wm = getSystemService(WALLPAPER_SERVICE) as android.app.WallpaperManager
            wm.setBitmap(bmp)
            toast("🖼️ Wallpaper cambiado")
        } catch (e: Exception) {
            toast("❌ No se pudo cambiar wallpaper: ${e.message}")
        }
    }

    private fun toggleDarkMode() {
        val isDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        )
        toast(if (isDark) "☀️ Modo claro" else "🌙 Modo oscuro")
    }

    private fun readSensors() {
        val sm = sensorManager ?: return
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel == null) { toast("❌ Sin acelerómetro"); return }
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        binding.root.postDelayed({
            sm.unregisterListener(this)
            showDialog("🧭 Acelerómetro", "X: $accelValues\nÚltima lectura registrada")
        }, 800)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            accelValues = "%.2f, %.2f, %.2f".format(it.values[0], it.values[1], it.values[2])
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun showSystemAll() {
        val msg = """
            ${SystemTools.deviceInfo()}
            
            ${SystemTools.batteryStatusText(this)}
            
            🌐 Red: ${SystemTools.networkType(this)}
            
            ${SystemTools.ramInfo()}
            
            ☀️ Brillo actual: ${(SystemTools.getBrightness(this) * 100) / 255}%
            
            🔢 Contador persistente: $counter
        """.trimIndent()
        showDialog("📊 Sistema completo", msg)
    }

    // ══════════════════════════════════════════════════════════════
    //  CONFIGURACIÓN
    // ══════════════════════════════════════════════════════════════
    private fun showSettingsDialog() {
        val items = arrayOf(
            "🔄 Actualizar Widget ahora",
            "🧹 Limpiar todas las notificaciones",
            "🪟 Estado de la ventana flotante",
            "🔔 Probar los 4 tipos de notificación",
            "⚙️ Abrir ajustes de permisos del sistema",
            "🔦 Abrir ajustes overlay",
            "ℹ️ Acerca de la app"
        )
        AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración")
            .setItems(items) { _, i ->
                when (i) {
                    0 -> { forceWidgetUpdate(this); toast("✅ Widget actualizado") }
                    1 -> clearAllNotifications()
                    2 -> checkFloatingStatus()
                    3 -> { showSimpleNotification(); showImageNotification(); showActionNotification(); showVibrateNotification() }
                    4 -> openAppSystemSettings()
                    5 -> openOverlaySettings()
                    6 -> showAbout()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun checkFloatingStatus() {
        val ok = canDrawOverlays()
        showDialog("🪟 Ventana Flotante",
            if (ok) "✅ Permiso concedido.\nToca \"Iniciar Ventana Flotante\"."
            else "❌ Falta permiso de overlay.\nConcede \"Dibujar sobre otras apps\".")
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    private fun openAppSystemSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun showAbout() {
        showDialog("AppNativaPro v2.0", """
            🎯 Laboratorio profesional con:
            
            • Ventana flotante interactiva (overlay)
            • 3 widgets (básico, rápido, panel-app)
            • 4 tipos de notificaciones
            • Quick Settings Tile
            • Servicio en primer plano
            • Picture-in-Picture con botones
            • WorkManager periódico
            • Deep Links + Shortcuts
            • Linterna, TTS, brillo, volumen
            • Generador de QR
            • Info del sistema (batería, red, RAM)
            • Sensores (acelerómetro)
            • Contador persistente
        """.trimIndent())
    }

    private fun runAllFeatures() {
        showSimpleNotification()
        showImageNotification()
        showActionNotification()
        showVibrateNotification()
        forceWidgetUpdate(this)
        SystemTools.fancyVibrate(this)
        toast("🎯 Se probaron las funciones principales")
    }

    // ══════════════════════════════════════════════════════════════
    //  PIP
    // ══════════════════════════════════════════════════════════════
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(9, 16))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val playPi = PendingIntent.getBroadcast(this, 1,
                    Intent(this, PipActionReceiver::class.java).apply { action = "ACTION_PLAY" },
                    PendingIntent.FLAG_IMMUTABLE)
                val closePi = PendingIntent.getBroadcast(this, 2,
                    Intent(this, PipActionReceiver::class.java).apply { action = "ACTION_CLOSE" },
                    PendingIntent.FLAG_IMMUTABLE)
                builder.setActions(listOf(
                    RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_media_play), "Play", "Play", playPi),
                    RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Cerrar", "Cerrar", closePi)
                ))
            }
            enterPictureInPictureMode(builder.build())
        } else toast("PiP requiere Android 8.0+")
    }

    // ══════════════════════════════════════════════════════════════
    //  NOTIFICACIONES
    // ══════════════════════════════════════════════════════════════
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel("canal_general", "General", NotificationManager.IMPORTANCE_HIGH))
            mgr.createNotificationChannel(NotificationChannel("canal_vibrate", "Vibración", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            })
        }
    }

    private fun showSimpleNotification() {
        val n = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación Simple")
            .setContentText("¡Funciona! ✔")
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(1, n)
    }

    private fun showImageNotification() {
        val bmp = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_camera) ?: return
        val n = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación con Imagen")
            .setContentText("Mira esto 👀").setLargeIcon(bmp)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp).bigLargeIcon(null as Bitmap?))
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(2, n)
    }

    private fun showActionNotification() {
        val pi1 = PendingIntent.getActivity(this, 10, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val pi2 = PendingIntent.getActivity(this, 11, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación con Botones")
            .setContentText("Presiona abajo 👇")
            .addAction(android.R.drawable.ic_menu_send, "Botón 1", pi1)
            .addAction(android.R.drawable.ic_menu_share, "Botón 2", pi2)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(3, n)
    }

    private fun showVibrateNotification() {
        val n = NotificationCompat.Builder(this, "canal_vibrate")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Vibración 📳").setContentText("Vibrando...")
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(4, n)
    }

    private fun clearAllNotifications() {
        getSystemService(NotificationManager::class.java).cancelAll()
        toast("🧹 Notificaciones limpiadas")
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ══════════════════════════════════════════════════════════════
    private fun shareText(text: String) {
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, text); type = "text/plain"
        }, "Compartir"))
    }

    private fun shareImage() {
        val uri = Uri.parse("android.resource://$packageName/${android.R.drawable.ic_menu_camera}")
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_STREAM, uri); type = "image/*"
        }, "Compartir Imagen"))
    }

    private fun schedulePeriodicWork() {
        val req = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(req)
        toast("⏱️ Tarea programada cada 15 min")
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host == "saloncopoya.com" && data.path?.startsWith("/proapp") == true) {
            toast("🔗 Deep Link: ${data.path}")
        }
    }

    private fun showDialog(title: String, msg: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }
}

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
