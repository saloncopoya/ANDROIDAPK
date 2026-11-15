package com.saloncopoya.proapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.saloncopoya.proapp.service.MyForegroundService
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        askPermissions()
        setupButtonListeners()
        createNotificationChannels()
        handleDeepLink(intent)
    }

    private fun askPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun setupButtonListeners() {
        binding.btnNotifSimple.setOnClickListener { showSimpleNotification() }
        binding.btnNotifImage.setOnClickListener { showImageNotification() }
        binding.btnNotifActions.setOnClickListener { showActionNotification() }
        binding.btnNotifVibrate.setOnClickListener { showVibrateNotification() }
        binding.btnStartService.setOnClickListener {
            val intent = Intent(this, MyForegroundService::class.java)
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "Servicio Iniciado", Toast.LENGTH_SHORT).show()
        }
        binding.btnPip.setOnClickListener { enterPipMode() }
        binding.btnScan.setOnClickListener { Toast.makeText(this, "Base para CameraX", Toast.LENGTH_SHORT).show() }
        binding.btnShareText.setOnClickListener { shareText("¡Mira esta app profesional!") }
        binding.btnShareImage.setOnClickListener { shareImage() }
        binding.btnWorkManager.setOnClickListener { schedulePeriodicWork() }
    }

    // 🔥 NUEVO PIP CON BOTONES INTERACTIVOS
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val playIntent = PendingIntent.getBroadcast(this, 1, Intent(this, PipActionReceiver::class.java).apply { action = "ACTION_PLAY" }, PendingIntent.FLAG_IMMUTABLE)
                val closeIntent = PendingIntent.getBroadcast(this, 2, Intent(this, PipActionReceiver::class.java).apply { action = "ACTION_CLOSE" }, PendingIntent.FLAG_IMMUTABLE)

                val playAction = RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_media_play), "Play", "Play", playIntent)
                val closeAction = RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Cerrar", "Cerrar", closeIntent)

                builder.setActions(listOf(playAction, closeAction))
            }
            enterPictureInPictureMode(builder.build())
        } else {
            Toast.makeText(this, "PiP requiere Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("canal_general", "General", NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel("canal_vibrate", "Vibración", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            })
        }
    }

    private fun showSimpleNotification() {
        val notification = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Notificación Simple").setContentText("Prueba.").setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    private fun showImageNotification() {
        val bitmap = BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_camera) ?: return
        val notification = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Notificación con Imagen").setContentText("Mira esto.")
            .setLargeIcon(bitmap).setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?)).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(2, notification)
    }

    private fun showActionNotification() {
        val pendingIntent1 = PendingIntent.getActivity(this, 10, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val pendingIntent2 = PendingIntent.getActivity(this, 11, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "canal_general")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Notificación con Botones").setContentText("Presiona abajo.")
            .addAction(android.R.drawable.ic_menu_send, "Botón 1", pendingIntent1).addAction(android.R.drawable.ic_menu_share, "Botón 2", pendingIntent2)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(3, notification)
    }

    private fun showVibrateNotification() {
        val notification = NotificationCompat.Builder(this, "canal_vibrate")
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Vibración").setContentText("Vibrando...").setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(4, notification)
    }

    private fun shareText(text: String) {
        startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, text); type = "text/plain" }, "Compartir"))
    }

    private fun shareImage() {
        val uri = Uri.parse("android.resource://$packageName/${android.R.drawable.ic_menu_camera}")
        startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_STREAM, uri); type = "image/*" }, "Compartir Imagen"))
    }

    private fun schedulePeriodicWork() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
        Toast.makeText(this, "Tarea programada", Toast.LENGTH_SHORT).show()
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.host == "saloncopoya.com" && data.path?.startsWith("/proapp") == true) {
            Toast.makeText(this, "Deep Link recibido", Toast.LENGTH_LONG).show()
        }
    }
}

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
