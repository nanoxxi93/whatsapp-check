package com.whatscheck.engines.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.whatscheck.engines.MainActivity
import com.whatscheck.engines.R

/** Canales y notificaciones: resultado de la llamada (heads-up, RF-05) y servicio "siempre activo" (RF-08). */
object Notifier {
    const val CHANNEL_RESULT = "resultado_llamada"
    const val CHANNEL_SERVICE = "servicio_activo"
    const val ID_SERVICE = 1
    private const val ID_RESULT = 2

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RESULT, "Resultado de la llamada", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Si el número que llama tiene WhatsApp (cuando no se puede mostrar el aviso flotante)"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, "WhatsCheck activo", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Mantiene la app lista para las llamadas entrantes. No suena ni aparece como emergente."
                setShowBadge(false)
            },
        )
    }

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    fun result(context: Context, text: String) {
        ensureChannels(context)
        val n = Notification.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.drawable.ic_stat_whatscheck)
            .setContentTitle("WhatsCheck")
            .setContentText(text)
            .setCategory(Notification.CATEGORY_STATUS)
            .setTimeoutAfter(60_000)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_RESULT, n)
    }

    fun service(context: Context): Notification {
        ensureChannels(context)
        return Notification.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_whatscheck)
            .setContentTitle("WhatsCheck activo")
            .setContentText("Revisa si las llamadas de desconocidos tienen WhatsApp")
            .setOngoing(true)
            .setContentIntent(openApp(context))
            .build()
    }
}
