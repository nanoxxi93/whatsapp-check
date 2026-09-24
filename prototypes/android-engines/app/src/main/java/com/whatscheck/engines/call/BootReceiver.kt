package com.whatscheck.engines.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reinicia el modo "siempre activo" al encender el teléfono (RF-08, CA-12). En MIUI requiere "Inicio automático". */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Journal.health(context, "boot")
            KeepAliveService.start(context, "encendido del teléfono")
        }
    }
}
