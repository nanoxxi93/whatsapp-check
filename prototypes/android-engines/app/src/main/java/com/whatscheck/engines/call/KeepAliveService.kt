package com.whatscheck.engines.call

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Modo "siempre activo" (RF-08): servicio en primer plano con notificación de prioridad mínima, para que la
 * optimización de batería (sobre todo en MIUI) no cierre la app entre llamadas. Anota un latido por hora en
 * health.jsonl (RF-10): si el sistema lo mata, se ve como un hueco en el registro.
 */
class KeepAliveService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val beat = object : Runnable {
        override fun run() {
            Journal.health(this@KeepAliveService, "heartbeat")
            main.postDelayed(this, HEARTBEAT_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: if (intent == null) "reinicio del sistema (sticky)" else "app"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(Notifier.ID_SERVICE, Notifier.service(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(Notifier.ID_SERVICE, Notifier.service(this))
            }
        } catch (e: Exception) {
            // Android 12+ puede negar iniciar un servicio en primer plano desde segundo plano
            Journal.health(this, "service_denied", "${e.javaClass.simpleName} ($reason)")
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            running = true
            Journal.health(this, "service_start", reason)
            main.postDelayed(beat, HEARTBEAT_MS)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Journal.health(this, "task_removed", "la app se quitó de recientes")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        running = false
        main.removeCallbacks(beat)
        Journal.health(this, "service_stop")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_REASON = "reason"
        private const val HEARTBEAT_MS = 60 * 60 * 1000L
        @Volatile private var running = false

        fun start(context: Context, reason: String) {
            val intent = Intent(context, KeepAliveService::class.java).putExtra(EXTRA_REASON, reason)
            runCatching { context.startForegroundService(intent) }
                .onFailure { Journal.health(context, "service_denied", "${it.javaClass.simpleName} ($reason)") }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }

        fun isRunning() = running
    }
}
