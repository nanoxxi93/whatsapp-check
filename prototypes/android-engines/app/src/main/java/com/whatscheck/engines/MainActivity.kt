package com.whatscheck.engines

import android.Manifest
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.whatscheck.engine.WhatsAppPresenceEngine
import com.whatscheck.engines.call.CallCheck
import com.whatscheck.engines.call.Journal
import com.whatscheck.engines.call.KeepAliveService
import com.whatscheck.engines.call.OverlayController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Se incrementa en cada onResume para refrescar el estado de permisos y el resumen. */
    private var resumeTick by mutableIntStateOf(0)

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resumeTick++ }
    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Prefs.keepAlive(this)) KeepAliveService.start(this, "app")
        val engines = Engines.available(applicationContext)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Column(
                        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("WhatsCheck · pruebas", style = MaterialTheme.typography.titleLarge)
                        Text("Usar solo con un número secundario de pruebas.", style = MaterialTheme.typography.bodySmall)
                        CallScreeningSection(
                            tick = resumeTick,
                            requestRole = ::requestRole,
                            requestNotifications = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                        HorizontalDivider()
                        EngineSection(engines)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }
}

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("whatscheck", Context.MODE_PRIVATE)
    fun keepAlive(c: Context) = p(c).getBoolean("keepAlive", true)
    fun setKeepAlive(c: Context, on: Boolean) = p(c).edit().putBoolean("keepAlive", on).apply()
}

/** Estado de rol y permisos, modo "siempre activo", guía de MIUI, aviso de ejemplo, resumen y exportación. */
@Composable
private fun CallScreeningSection(tick: Int, requestRole: () -> Unit, requestNotifications: () -> Unit) {
    val context = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") tick // leer tick fuerza a recalcular todo en cada onResume
    val role = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    val overlayOk = Settings.canDrawOverlays(context)
    val notifOk = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val batteryOk = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    var keepAlive by remember { mutableStateOf(Prefs.keepAlive(context)) }
    val summary = remember(tick) { Journal.summary(context) }
    val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    Text("Filtrado de llamadas", style = MaterialTheme.typography.titleMedium)
    StatusRow("Rol de filtrado de llamadas", role, "Pedir", requestRole)
    StatusRow("Aviso flotante sobre otras apps", overlayOk, "Permitir") {
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
    }
    StatusRow("Notificaciones", notifOk, "Permitir", requestNotifications)
    StatusRow("Batería sin restricciones", batteryOk, "Excluir") {
        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Siempre activo (notificación fija)", Modifier.weight(1f))
        Switch(checked = keepAlive, onCheckedChange = {
            keepAlive = it
            Prefs.setKeepAlive(context, it)
            if (it) KeepAliveService.start(context, "activado por el usuario") else KeepAliveService.stop(context)
        })
    }

    if (isXiaomi) {
        Text(
            "Xiaomi / MIUI: activa también \"Inicio automático\", en Ahorro de batería elige \"Sin restricciones\", " +
                "y en Otros permisos permite \"Mostrar ventanas emergentes mientras se ejecuta en segundo plano\" " +
                "y \"Mostrar en pantalla de bloqueo\".",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { openMiui(context, autostart = false) }) { Text("Otros permisos") }
            OutlinedButton(onClick = { openMiui(context, autostart = true) }) { Text("Inicio automático") }
        }
    }

    OutlinedButton(enabled = overlayOk, onClick = {
        val overlay = CallCheck.overlay(context)
        overlay.show(OverlayController.Kind.CHECKING, "Aviso de ejemplo")
        Handler(Looper.getMainLooper()).postDelayed({ overlay.show(OverlayController.Kind.HAS, "Aviso de ejemplo · toca para cerrar") }, 2_000)
    }) { Text("Mostrar aviso de ejemplo") }

    Text("Registro", style = MaterialTheme.typography.titleMedium)
    SelectionContainer { Text(summary.asText(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Resumen WhatsCheck", summary.asText()))
        }) { Text("Copiar resumen") }
        Button(onClick = { context.startActivity(Intent.createChooser(Journal.shareIntent(context), "Exportar registro")) }) {
            Text("Exportar")
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${if (ok) "✅" else "⬜"}  $label", Modifier.weight(1f))
        if (!ok) OutlinedButton(onClick = onClick) { Text(action) }
    }
}

/** Pantallas propias de MIUI; si no existen, abre los ajustes de la app. */
private fun openMiui(context: Context, autostart: Boolean) {
    val intent = if (autostart) {
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
    } else {
        Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
}

/** Pruebas del motor: elegir motor, vincular, consultar y desvincular. Solo para números secundarios. */
@Composable
private fun EngineSection(engines: List<WhatsAppPresenceEngine>) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val log = remember { mutableStateListOf<String>() }
    var selected by remember { mutableStateOf(engines.firstOrNull()) }
    var phone by remember { mutableStateOf("") }
    fun say(line: String) = log.add(0, line)

    Text("Motor", style = MaterialTheme.typography.titleMedium)
    if (engines.isEmpty()) {
        Text("Todavía no hay motores compilados en esta app.")
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        engines.forEach { e -> FilterChip(selected = e == selected, onClick = { selected = e }, label = { Text(e.id) }) }
    }
    val engine = selected ?: return
    val state by engine.sessionState.collectAsState()
    Text("Sesión: $state")

    OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Número (E.164 sin +)") })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            scope.launch {
                runCatching { engine.requestPairingCode(phone) }.fold({ code ->
                    // El usuario vincula desde el mismo teléfono: el código se copia para pegarlo en WhatsApp
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Código de WhatsApp", code))
                    say("Código: $code (copiado al portapapeles)")
                }, { say("Error: ${it.message}") })
            }
        }) { Text("Vincular") }
        Button(onClick = {
            scope.launch {
                runCatching {
                    val t = engine.connect()
                    val r = engine.check(phone)
                    engine.close()
                    "${Phone.mask(phone)} → ${r.status}${r.reason?.let { " ($it)" } ?: ""} · motor ${t.tEngineMs} ms · sesión ${t.tReadyMs} ms"
                }.fold(::say) { say("Error: ${it.message?.let(Phone::maskInText)}") }
            }
        }) { Text("Consultar") }
        Button(onClick = { scope.launch { runCatching { engine.unlink() }.fold({ say("Desvinculado") }, { say("Error: ${it.message}") }) } }) {
            Text("Desvincular")
        }
    }
    SelectionContainer {
        Column { log.forEach { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } }
    }
}
