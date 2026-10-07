package com.whatscheck.engines.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatscheck.engines.BuildConfig
import com.whatscheck.engines.Prefs
import com.whatscheck.engines.call.CallCheck
import com.whatscheck.engines.call.KeepAliveService
import com.whatscheck.engines.call.OverlayController
import com.whatscheck.engines.whatsmeow.WhatsmeowEngine
import kotlinx.coroutines.launch

/** Ajustes: permisos, MIUI, funcionamiento, cuenta y versión (RF-05). */
@Composable
fun SettingsScreen(
    engine: WhatsmeowEngine,
    tick: Int,
    requestRole: () -> Unit,
    requestNotifications: () -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val setup = remember(tick) { Setup.read(context) }
    var keepAlive by remember { mutableStateOf(Prefs.keepAlive(context)) }
    var overlayDuration by remember(tick) { mutableIntStateOf(Prefs.overlayDurationSeconds(context)) }
    var durationExpanded by remember { mutableStateOf(false) }
    var confirmUnlink by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SectionCard("Permisos") {
            PermissionRow("Filtrado de llamadas", "Android le pasa a WhatsCheck el número de quien llama, solo si no está en tus contactos.", setup.role, "Activar", requestRole)
            PermissionRow("Aviso flotante", "Muestra el resultado sobre la pantalla de la llamada.", setup.overlay, "Permitir") { Setup.openOverlaySettings(context) }
            PermissionRow("Notificaciones", "Plan B si no se puede mostrar el aviso flotante.", setup.notifications, "Permitir", requestNotifications)
            PermissionRow("Batería sin restricciones", "Evita que el sistema cierre la app entre llamadas.", setup.battery, "Excluir") { Setup.openBatteryExemption(context) }
        }

        if (Setup.isXiaomi) {
            SectionCard("Xiaomi / MIUI") {
                Text(
                    "MIUI cierra apps en segundo plano con más fuerza. Activa:\n" +
                        "• Inicio automático\n" +
                        "• Ahorro de batería → Sin restricciones\n" +
                        "• Otros permisos → Ventanas emergentes en segundo plano y Pantalla de bloqueo",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { Setup.openMiui(context, autostart = true) }) { Text("Inicio automático") }
                    OutlinedButton(onClick = { Setup.openMiui(context, autostart = false) }) { Text("Otros permisos") }
                }
            }
        }

        SectionCard("Funcionamiento") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Siempre activo", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Notificación fija y discreta para que la app siga lista entre llamadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = keepAlive, onCheckedChange = {
                    keepAlive = it
                    Prefs.setKeepAlive(context, it)
                    if (it) KeepAliveService.start(context, "activado por el usuario") else KeepAliveService.stop(context)
                    Handler(Looper.getMainLooper()).postDelayed(onChanged, 300)
                })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Duración del aviso", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Tiempo que permanece el resultado en pantalla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    OutlinedButton(onClick = { durationExpanded = true }) {
                        Text("$overlayDuration s ▼")
                    }
                    DropdownMenu(
                        expanded = durationExpanded,
                        onDismissRequest = { durationExpanded = false },
                    ) {
                        val options = listOf(
                            5 to "5 segundos",
                            8 to "8 segundos (recomendado)",
                            12 to "12 segundos",
                        )
                        options.forEach { (sec, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        label,
                                        fontWeight = if (sec == overlayDuration) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    overlayDuration = sec
                                    Prefs.setOverlayDurationSeconds(context, sec)
                                    durationExpanded = false
                                    onChanged()
                                },
                            )
                        }
                    }
                }
            }
            OutlinedButton(enabled = setup.overlay, onClick = {
                val overlay = CallCheck.overlay(context)
                val durationSec = Prefs.overlayDurationSeconds(context)
                overlay.show(OverlayController.Kind.CHECKING, "Aviso de ejemplo")
                Handler(Looper.getMainLooper()).postDelayed({
                    overlay.show(
                        OverlayController.Kind.HAS,
                        "Aviso de ejemplo (${durationSec} s) · toca para cerrar",
                    )
                }, 1_500)
            }) { Text("Mostrar aviso de ejemplo") }
        }

        SectionCard("Cuenta") {
            Text(
                "Desvincular cierra la sesión de WhatsCheck en tu WhatsApp y borra lo guardado en este teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { confirmUnlink = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Desvincular WhatsApp") }
        }

        SectionCard("Acerca de") {
            Text("WhatsCheck ${BuildConfig.VERSION_NAME} · prototipo de la Fase 0", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Muestra si quien te llama tiene WhatsApp. No detecta spam, no lee tus mensajes y no te marca \"en línea\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmUnlink) {
        AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text("¿Desvincular WhatsApp?") },
            text = { Text("WhatsCheck dejará de revisar las llamadas hasta que vuelvas a vincular.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnlink = false
                    scope.launch {
                        runCatching { engine.unlink() }
                        onChanged()
                    }
                }) { Text("Desvincular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmUnlink = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PermissionRow(title: String, description: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (ok) "✅" else "⬜")
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!ok) OutlinedButton(onClick = onClick) { Text(action) }
    }
}
