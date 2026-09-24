package com.whatscheck.engines.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatscheck.engine.CheckStatus
import com.whatscheck.engine.SessionState
import com.whatscheck.engines.Phone
import com.whatscheck.engines.call.CallCheck
import com.whatscheck.engines.call.Journal
import com.whatscheck.engines.whatsmeow.WhatsmeowEngine
import kotlinx.coroutines.launch

/** Inicio: cuenta vinculada, estado de la protección, consulta manual y últimas llamadas (RF-02, RF-03). */
@Composable
fun HomeScreen(engine: WhatsmeowEngine, tick: Int, onOpenSettings: () -> Unit, onOpenLog: () -> Unit) {
    val context = LocalContext.current
    val state by engine.sessionState.collectAsState()
    var linkedPhone by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // Estado real al abrir y en cada onResume (antes quedaba en UNLINKED hasta la primera consulta)
    LaunchedEffect(tick, state) {
        runCatching { engine.refresh() }
        linkedPhone = runCatching { engine.linkedPhone() }.getOrDefault("")
        loaded = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("WhatsCheck", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("¿Tiene WhatsApp quien te llama?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            !loaded -> CircularProgressIndicator()
            state == SessionState.UNLINKED -> LinkCard(engine)
            else -> {
                AccountCard(state, linkedPhone)
                ProtectionCard(Setup.read(context), onOpenSettings)
                LookupCard()
                RecentCalls(tick, onOpenLog)
            }
        }
    }
}

@Composable
private fun AccountCard(state: SessionState, digits: String) {
    var reveal by remember { mutableStateOf(false) }
    val (label, color) = when (state) {
        SessionState.READY -> "Conectada ahora" to Brand.ok()
        SessionState.CONNECTING -> "Conectando…" to Brand.Unknown
        SessionState.CLOSED -> "Vinculada · lista" to Brand.ok()
        SessionState.LOGGED_OUT -> "Sesión cerrada: vuelve a vincular" to MaterialTheme.colorScheme.error
        SessionState.BANNED -> "Cuenta restringida por WhatsApp" to MaterialTheme.colorScheme.error
        SessionState.UNLINKED -> "Sin vincular" to Brand.Unknown
    }
    SectionCard("Cuenta de WhatsApp") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        digits.isEmpty() -> "—"
                        reveal -> Fmt.phone(digits)
                        else -> Phone.mask(digits)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                )
                Text("Se conecta solo cuando entra una llamada", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (digits.isNotEmpty()) TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Ocultar" else "Mostrar") }
        }
        Pill(label, color)
    }
}

@Composable
private fun ProtectionCard(setup: Setup, onOpenSettings: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (setup.complete) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (setup.complete) Brand.ok() else if (setup.protecting) Brand.warn() else MaterialTheme.colorScheme.error,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        setup.complete -> "Protección activa"
                        setup.protecting -> "Protección activa, con avisos"
                        else -> "Protección desactivada"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (setup.complete) "Revisamos las llamadas de números que no tienes guardados."
                    else "Falta: ${setup.missing.joinToString(", ")}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!setup.complete) OutlinedButton(onClick = onOpenSettings) { Text("Revisar en Ajustes") }
    }
}

@Composable
private fun LookupCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<ResultLook, String>?>(null) }

    SectionCard("Consultar un número") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                input,
                { input = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                Modifier.weight(1f),
                placeholder = { Text("987 654 321") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Button(enabled = !busy && input.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    val start = System.currentTimeMillis()
                    val digits = CallCheck.normalize(input)
                    val outcome = CallCheck.verify(context, digits)
                    val look = when (outcome.result.status) {
                        CheckStatus.HAS_WHATSAPP -> ResultLook.HAS
                        CheckStatus.NO_WHATSAPP -> ResultLook.NO
                        CheckStatus.UNKNOWN -> ResultLook.UNKNOWN
                    }
                    result = look to "${digits?.let(Phone::mask) ?: "Número no válido"} · ${Fmt.seconds(System.currentTimeMillis() - start)}"
                    busy = false
                }
            }) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.Filled.Search, contentDescription = "Consultar")
            }
        }
        result?.let { (look, detail) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${look.emoji} ${look.label}", look.color())
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecentCalls(tick: Int, onOpenLog: () -> Unit) {
    val context = LocalContext.current
    val calls = remember(tick) { Journal.calls(context).take(3) }
    SectionCard("Últimas llamadas") {
        if (calls.isEmpty()) {
            Text(
                "Todavía no llegó ninguna llamada de un número desconocido.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            calls.forEach { CallRow(it) }
            TextButton(onClick = onOpenLog) { Text("Ver registro completo") }
        }
    }
}

/** Vinculación guiada cuando no hay sesión (RF-03). */
@Composable
private fun LinkCard(engine: WhatsmeowEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    SectionCard("Vincula tu WhatsApp") {
        Text(
            "WhatsCheck usa tu propia cuenta de WhatsApp, vinculada como un dispositivo más, para saber si quien te llama " +
                "tiene WhatsApp. No lee ni guarda tus mensajes.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Usa un cliente no oficial: WhatsApp podría restringir la cuenta. Durante las pruebas, usa un número secundario.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        if (code == null) {
            OutlinedTextField(
                phone,
                { phone = it.filter(Char::isDigit) },
                Modifier.fillMaxWidth(),
                label = { Text("Tu número con código de país (51…)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Button(enabled = !busy && phone.length >= 8, onClick = {
                scope.launch {
                    busy = true
                    error = null
                    runCatching { engine.requestPairingCode(phone) }
                        .onSuccess {
                            code = it
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Código de WhatsApp", it))
                        }
                        .onFailure { error = it.message?.let(Phone::maskInText) }
                    busy = false
                }
            }) { Text(if (busy) "Pidiendo código…" else "Obtener código") }
        } else {
            val pretty = code!!.let { if (it.length == 8) "${it.take(4)}-${it.drop(4)}" else it }
            Text(pretty, fontSize = 36.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Copiado al portapapeles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "1. Abre WhatsApp → Dispositivos vinculados\n2. Vincular un dispositivo\n3. \"Vincular con el número de teléfono\"\n4. Pega o escribe el código",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Text("Esperando que ingreses el código…", style = MaterialTheme.typography.bodySmall)
            }
        }
        error?.let { Text("No se pudo: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
