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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.saloncopoya.proapp.databinding.ActivityMainBinding
import com.saloncopoya.proapp.service.FloatingWindowService
import com.saloncopoya.proapp.service.MyForegroundService
import com.saloncopoya.proapp.widget.forceWidgetUpdate
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val OVERLAY_REQ = 501

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { !it }) {
            Toast.makeText(this, "⚠️ Algunos permisos fueron denegados", Toast.LENGTH_SHORT).show()
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

        // Si vino con extra "enter_pip", entrar en PiP automáticamente
        if (intent.getBooleanExtra("enter_pip", false)) {
            binding.root.postDelayed({ enterPipMode() }, 400)
        }
    }

    private fun askPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun setupButtons() {
        // ── VENTANA FLOTANTE
        binding.btnFloatingStart.setOnClickListener { startFloatingWindow() }
        binding.btnFloatingStop.setOnClickListener { stopFloatingWindow() }

        // ── NOTIFICACIONES
        binding.btnNotifSimple.setOnClickListener { showSimpleNotification() }
        binding.btnNotifImage.setOnClickListener { showImageNotification() }
        binding.btnNotifActions.setOnClickListener { showActionNotification() }
        binding.btnNotifVibrate.setOnClickListener { showVibrateNotification() }
        binding.btnClearNotifs.setOnClickListener { clearAllNotifications() }

        // ── SISTEMA
        binding.btnStartService.setOnClickListener {
            ContextCompat.startForegroundService(this, Intent(this, MyForegroundService::class.java))
            Toast.makeText(this, "▶️ Servicio iniciado", Toast.LENGTH_SHORT).show()
        }
        binding.btnPip.setOnClickListener { enterPipMode() }
        binding.btnWorkManager.setOnClickListener { schedulePeriodicWork() }

        // ── HERRAMIENTAS
        binding.btnScan.setOnClickListener {
            Toast.makeText(this, "📷 Base CameraX lista (integra ScanActivity)", Toast.LENGTH_SHORT).show()
        }
        binding.btnShareText.setOnClickListener { shareText("¡Mira esta app profesional!") }
        binding.btnShareImage.setOnClickListener { shareImage() }

        // ── CONFIGURACIÓN
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
        binding.btnTestAll.setOnClickListener { runAllFeatures() }
    }

    // ══════════════════════════════════════════════════════════════
    //  VENTANA FLOTANTE
    // ══════════════════════════════════════════════════════════════
    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

    private fun startFloatingWindow() {
        if (!canDrawOverlays()) {
            Toast.makeText(this, getString(R.string.msg_overlay_needed), Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivityForResult(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ), OVERLAY_REQ
                )
            }
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, FloatingWindowService::class.java))
        Toast.makeText(this, getString(R.string.msg_floating_started), Toast.LENGTH_LONG).show()
        // Enviar la app a segundo plano para que veas la ventana flotante
        binding.root.postDelayed({ moveTaskToBack(true) }, 500)
    }

    private fun stopFloatingWindow() {
        stopService(Intent(this, FloatingWindowService::class.java))
        Toast.makeText(this, getString(R.string.msg_floating_stopped), Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQ && canDrawOverlays()) {
            startFloatingWindow()
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CONFIGURACIÓN (diálogo con opciones)
    // ══════════════════════════════════════════════════════════════
    private fun showSettingsDialog() {
        val items = arrayOf(
            "🔄 Actualizar Widget ahora",
            "🧹 Limpiar todas las notificaciones",
            "🪟 Estado de la ventana flotante",
            "🔔 Probar los 4 tipos de notificación",
            "⚙️ Abrir ajustes de permisos del sistema",
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
                    5 -> showAbout()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun checkFloatingStatus() {
        val overlayOk = canDrawOverlays()
        val msg = if (overlayOk)
            "✅ Permiso de overlay concedido.\nToca \"Iniciar Ventana Flotante\" para mostrarla."
        else
            "❌ Falta el permiso de overlay.\nConcede \"Dibujar sobre otras apps\" en ajustes."
        AlertDialog.Builder(this)
            .setTitle("🪟 Ventana Flotante")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .setNeutralButton("Abrir ajustes") { _, _ -> openOverlaySettings() }
            .show()
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
    }

    private fun openAppSystemSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")))
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("AppNativaPro v1.0")
            .setMessage("App demo con:\n\n• Ventana flotante interactiva (overlay)\n• 4 tipos de notificaciones\n• 2 widgets Glance\n• Quick Settings Tile\n• Servicio en primer plano\n• Picture-in-Picture con botones\n• WorkManager periódico\n• Deep Links")
            .setPositiveButton("Genial", null)
            .show()
    }

    private fun runAllFeatures() {
        showSimpleNotification()
        showImageNotification()
        showActionNotification()
        showVibrateNotification()
        forceWidgetUpdate(this)
        toast("🎯 Se probaron todas las funciones")
    }

    // ══════════════════════════════════════════════════════════════
    //  PIP (video flotante)
    // ══════════════════════════════════════════════════════════════
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val playPi = PendingIntent.getBroadcast(this, 1,
                    Intent(this, PipActionReceiver::class.java).apply { action = "ACTION_PLAY" },
                    PendingIntent.FLAG_IMMUTABLE)
                val closePi = PendingIntent.getBroadcast(this, 2,
                    Intent(this, PipActionReceiver::class.java).apply { action = "ACTION_CLOSE" },
                    PendingIntent.FLAG_IMMUTABLE)

                builder.setActions(listOf(
                    RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_media_play),
                        "Play", "Play", playPi),
                    RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                        "Cerrar", "Cerrar", closePi)
                ))
            }
            enterPictureInPictureMode(builder.build())
        } else {
            toast("PiP requiere Android 8.0+")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  NOTIFICACIONES
    // ══════════════════════════════════════════════════════════════
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel("canal_general", "General", NotificationManager.IMPORTANCE_HIGH)
            )
            mgr.createNotificationChannel(
                NotificationChannel("canal_vibrate", "Vibración", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
            )
        }
    }

    private fun showSimpleNotification() {
        val n = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación Simple")
            .setContentText("¡Funciona! ✔")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(1, n)
    }

    private fun showImageNotification() {
        val bmp = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_camera) ?: return
        val n = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación con Imagen")
            .setContentText("Mira esto 👀")
            .setLargeIcon(bmp)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp).bigLargeIcon(null as Bitmap?))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(2, n)
    }

    private fun showActionNotification() {
        val pi1 = PendingIntent.getActivity(this, 10,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val pi2 = PendingIntent.getActivity(this, 11,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación con Botones")
            .setContentText("Presiona abajo 👇")
            .addAction(android.R.drawable.ic_menu_send, "Botón 1", pi1)
            .addAction(android.R.drawable.ic_menu_share, "Botón 2", pi2)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(3, n)
    }

    private fun showVibrateNotification() {
        val n = NotificationCompat.Builder(this, "canal_vibrate")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Vibración 📳")
            .setContentText("Vibrando...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(4, n)
    }

    private fun clearAllNotifications() {
        getSystemService(NotificationManager::class.java).cancelAll()
        toast("🧹 Notificaciones limpiadas")
    }

    // ══════════════════════════════════════════════════════════════
    //  COMPARTIR / WORKMANAGER / DEEPLINK
    // ══════════════════════════════════════════════════════════════
    private fun shareText(text: String) {
        startActivity(Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }, "Compartir"))
    }

    private fun shareImage() {
        val uri = Uri.parse("android.resource://$packageName/${android.R.drawable.ic_menu_camera}")
        startActivity(Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
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
            Toast.makeText(this, "🔗 Deep Link: ${data.path}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
