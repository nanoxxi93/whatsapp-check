package com.whatscheck.engines.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatscheck.engines.call.Journal

/** Registro: resumen, lista de llamadas filtradas y exportación sin PC (RF-04). */
@Composable
fun LogScreen(tick: Int) {
    val context = LocalContext.current
    val summary = remember(tick) { Journal.summary(context) }
    val calls = remember(tick) { Journal.calls(context) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Registro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SectionCard {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier) {
                Stat("${summary.calls}", "llamadas", Modifier.weight(1f))
                Stat("${summary.has}", "✅ con WA", Modifier.weight(1f))
                Stat("${summary.no}", "⚠️ sin WA", Modifier.weight(1f))
                Stat("${summary.unknown}", "❔ sin verificar", Modifier.weight(1f))
            }
            HorizontalDivider()
            Text(
                "Tiempo hasta el resultado: p50 ${Fmt.seconds(summary.p50ResultMs)} · p95 ${Fmt.seconds(summary.p95ResultMs)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Servicio: último latido ${Fmt.whenIso(summary.lastHeartbeat)} · arranques ${summary.serviceStarts}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Problemas de sesión: ${if (summary.sessionProblems.isEmpty()) "ninguno" else summary.sessionProblems.joinToString("; ")}",
                style = MaterialTheme.typography.bodySmall,
                color = if (summary.sessionProblems.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Resumen WhatsCheck", summary.asText()))
                    Toast.makeText(context, "Resumen copiado", Toast.LENGTH_SHORT).show()
                }) { Text("Copiar resumen") }
                Button(onClick = { context.startActivity(Intent.createChooser(Journal.shareIntent(context), "Exportar registro")) }) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Exportar")
                }
            }
        }

        SectionCard("Llamadas de desconocidos") {
            if (calls.isEmpty()) {
                Text(
                    "Aquí aparecerán las llamadas de números que no tienes guardados. Los números se muestran enmascarados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            calls.forEachIndexed { i, call ->
                if (i > 0) HorizontalDivider()
                CallRow(call)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
