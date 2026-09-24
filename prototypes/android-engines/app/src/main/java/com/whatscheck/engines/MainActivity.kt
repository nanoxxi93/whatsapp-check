package com.whatscheck.engines

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.whatscheck.engine.WhatsAppPresenceEngine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engines = Engines.available(applicationContext)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) { EngineScreen(engines) }
            }
        }
    }
}

/** Pantalla de pruebas: elegir motor, vincular, consultar y desvincular. Solo para números secundarios. */
@Composable
private fun EngineScreen(engines: List<WhatsAppPresenceEngine>) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val log = remember { mutableStateListOf<String>() }
    var selected by remember { mutableStateOf(engines.firstOrNull()) }
    var phone by remember { mutableStateOf("") }
    fun say(line: String) = log.add(0, line)

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("WhatsCheck · comparar motores", style = MaterialTheme.typography.titleLarge)
        Text("Usar solo con un número secundario de pruebas.", style = MaterialTheme.typography.bodySmall)

        if (engines.isEmpty()) {
            Text("Todavía no hay motores compilados en esta app.")
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            engines.forEach { e -> FilterChip(selected = e == selected, onClick = { selected = e }, label = { Text(e.id) }) }
        }
        val engine = selected ?: return@Column
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
                    }.fold(::say) { say("Error: ${it.message}") }
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
}
