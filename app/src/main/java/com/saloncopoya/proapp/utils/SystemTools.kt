package com.saloncopoya.proapp.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

object SystemTools {

    // ── Batería
    fun batteryLevel(ctx: Context): Int {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun batteryStatusText(ctx: Context): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        return "🔋 Batería: $level% ${if (charging) "⚡ Cargando" else "🔌 Descargando"}"
    }

    // ── Red
    fun networkType(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "❌ Sin red"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "📶 WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "📡 Datos móviles"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "🔌 Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "🛡️ VPN"
            else -> "❓ Desconocido"
        }
    }

    fun networkDetails(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Sin conexión"
        val down = caps.linkDownstreamBandwidthKbps / 1000
        val up = caps.linkUpstreamBandwidthKbps / 1000
        return "${networkType(ctx)}\n↓ ${down} Mbps  ↑ ${up} Mbps"
    }

    // ── RAM
    fun ramInfo(): String {
        val rt = Runtime.getRuntime()
        val totalMb = rt.totalMemory() / (1024 * 1024)
        val freeMb = rt.freeMemory() / (1024 * 1024)
        val usedMb = totalMb - freeMb
        return "💾 RAM: ${usedMb}MB usados / ${totalMb}MB totales"
    }

    // ── Dispositivo
    fun deviceInfo(): String = """
        📱 Fabricante: ${Build.MANUFACTURER}
        🔧 Modelo: ${Build.MODEL}
        🏷️ Marca: ${Build.BRAND}
        🧩 Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        🏗️ Build: ${Build.ID}
        📟 Hardware: ${Build.HARDWARE}
    """.trimIndent()

    // ── Volumen
    fun adjustVolume(ctx: Context, up: Boolean): Int {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val dir = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (cur * 100) / max
    }

    // ── Brillo (requiere WRITE_SETTINGS)
    fun hasWriteSettings(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(ctx)

    fun setBrightness(ctx: Context, value: Int) {
        try {
            Settings.System.putInt(
                ctx.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value.coerceIn(0, 255)
            )
        } catch (_: Exception) {}
    }

    fun getBrightness(ctx: Context): Int = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (_: Exception) { 128 }

    // ── Linterna
    private var torchOn = false
    fun toggleFlashlight(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        torchOn = !torchOn
        cm.setTorchMode(cameraId, torchOn)
        return torchOn
    }

    // ── Vibración con patrón
    fun fancyVibrate(ctx: Context) {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 100, 100, 200, 100, 300, 100, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION") v.vibrate(pattern, -1)
        }
    }

    // ── Portapapeles
    fun copyToClipboard(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("AppPro", text))
        Toast.makeText(ctx, "📋 Copiado: $text", Toast.LENGTH_SHORT).show()
    }

    fun pasteFromClipboard(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "(vacío)"
        Toast.makeText(ctx, "📄 Pegado: $text", Toast.LENGTH_LONG).show()
        return text
    }

    // ── QR Generator
    fun generateQr(text: String, size: Int = 512): Bitmap? = try {
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (_: Exception) { null }

    // ── TTS
    private var tts: TextToSpeech? = null
    fun speak(ctx: Context, text: String) {
        tts?.stop()
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "app_pro_tts")
            } else {
                Toast.makeText(ctx, "❌ TTS no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Abrir sistema
    fun openUrl(ctx: Context, url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(ctx, "❌ No se pudo abrir URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(ctx: Context, to: String, subject: String) {
        val i = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$to")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { ctx.startActivity(i) } catch (_: Exception) {
            Toast.makeText(ctx, "❌ Sin app de email", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialNumber(ctx: Context, number: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(ctx, "❌ No se puede marcar", Toast.LENGTH_SHORT).show()
        }
    }

    fun playClickSound(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.6f)
    }
}
