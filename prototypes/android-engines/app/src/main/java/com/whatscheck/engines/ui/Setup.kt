package com.whatscheck.engines.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.whatscheck.engines.call.KeepAliveService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Estado de lo que necesita la protección para funcionar (lo usan Inicio y Ajustes). */
data class Setup(
    val role: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val battery: Boolean,
    val keepAlive: Boolean,
) {
    /** Lo mínimo para que el aviso aparezca: el rol y (overlay o notificaciones). */
    val protecting get() = role && (overlay || notifications)
    val complete get() = role && overlay && notifications && battery && keepAlive

    val missing: List<String>
        get() = buildList {
            if (!role) add("rol de filtrado de llamadas")
            if (!overlay) add("aviso flotante")
            if (!notifications) add("notificaciones")
            if (!battery) add("batería sin restricciones")
            if (!keepAlive) add("modo siempre activo")
        }

    companion object {
        fun read(context: Context) = Setup(
            role = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
            overlay = Settings.canDrawOverlays(context),
            notifications = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            battery = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
            keepAlive = KeepAliveService.isRunning(),
        )

        val isXiaomi get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

        fun openOverlaySettings(context: Context) = context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
        )

        fun openBatteryExemption(context: Context) = context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
        )

        /** Pantallas propias de MIUI; si no existen, abre los ajustes de la app. */
        fun openMiui(context: Context, autostart: Boolean) {
            val intent = if (autostart) {
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            } else {
                Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName)
            }
            runCatching { context.startActivity(intent) }.onFailure {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }
        }
    }
}

object Fmt {
    private val time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    private val dayTime = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

    /** ISO → "hoy 14:32" / "ayer 09:10" / "21/09 18:05". */
    fun whenIso(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
        val day = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now()
        return when (day) {
            today -> "hoy ${time.format(instant)}"
            today.minusDays(1) -> "ayer ${time.format(instant)}"
            else -> dayTime.format(instant)
        }
    }

    fun seconds(ms: Long?): String = ms?.let { "%.1f s".format(it / 1000.0) } ?: "—"

    /** "51987654321" → "+51 987 654 321" (Perú); otros países: "+" y los dígitos. */
    fun phone(digits: String): String =
        if (digits.length == 11 && digits.startsWith("51")) "+51 ${digits.substring(2, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"
        else "+$digits"
}
