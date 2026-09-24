package com.whatscheck.engines.call

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.time.Instant
import kotlin.math.ceil

/**
 * Registro local de la semana de uso real (RF-06, RF-09, RF-10). Todo queda en filesDir/results y con
 * los números enmascarados. Se exporta desde la app con el menú de compartir: no hace falta la PC.
 *
 *  - calls.jsonl:  una línea por llamada filtrada.
 *  - health.jsonl: latidos cada hora, arranques del servicio y eventos de la sesión.
 */
object Journal {
    private const val CALLS = "calls.jsonl"
    private const val HEALTH = "health.jsonl"
    private val lock = Any()

    private fun dir(context: Context) = File(context.filesDir, "results").apply { mkdirs() }

    fun call(context: Context, record: JSONObject) = append(context, CALLS, record)

    fun health(context: Context, event: String, detail: String? = null) = append(
        context, HEALTH,
        JSONObject().put("event", event).put("detail", detail ?: JSONObject.NULL),
    )

    private fun append(context: Context, name: String, record: JSONObject) {
        if (!record.has("ts")) record.put("ts", Instant.now().toString())
        synchronized(lock) { File(dir(context), name).appendText(record.toString() + "\n") }
    }

    private fun read(context: Context, name: String): List<JSONObject> = synchronized(lock) {
        val f = File(dir(context), name)
        if (!f.exists()) emptyList() else f.readLines().filter { it.isNotBlank() }.mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
    }

    /** Llamadas filtradas, de la más reciente a la más antigua (números ya enmascarados). */
    fun calls(context: Context): List<JSONObject> = read(context, CALLS).reversed()

    data class Summary(
        val calls: Int,
        val has: Int,
        val no: Int,
        val unknown: Int,
        val p50ResultMs: Long?,
        val p95ResultMs: Long?,
        val lastCall: String?,
        val lastHeartbeat: String?,
        val serviceStarts: Int,
        val sessionProblems: List<String>,
    ) {
        fun asText(): String = buildString {
            appendLine("WhatsCheck · resumen del registro")
            appendLine("Llamadas filtradas: $calls (✅ $has · ⚠️ $no · ❔ $unknown)")
            appendLine("Tiempo hasta el resultado: p50 ${p50ResultMs?.let { "%.2f s".format(it / 1000.0) } ?: "—"} · p95 ${p95ResultMs?.let { "%.2f s".format(it / 1000.0) } ?: "—"}")
            appendLine("Última llamada: ${lastCall ?: "—"}")
            appendLine("Último latido del servicio: ${lastHeartbeat ?: "—"} · arranques: $serviceStarts")
            append("Problemas de sesión: ${if (sessionProblems.isEmpty()) "ninguno" else sessionProblems.joinToString("; ")}")
        }
    }

    fun summary(context: Context): Summary {
        val calls = read(context, CALLS)
        val health = read(context, HEALTH)
        val results = calls.mapNotNull { if (it.isNull("tResult")) null else it.getLong("tResult") }.sorted()
        fun pct(p: Int) = if (results.isEmpty()) null else results[(ceil(p / 100.0 * results.size).toInt().coerceIn(1, results.size)) - 1]
        return Summary(
            calls = calls.size,
            has = calls.count { it.optString("status") == "HAS_WHATSAPP" },
            no = calls.count { it.optString("status") == "NO_WHATSAPP" },
            unknown = calls.count { it.optString("status") == "UNKNOWN" },
            p50ResultMs = pct(50),
            p95ResultMs = pct(95),
            lastCall = calls.lastOrNull()?.optString("ts"),
            lastHeartbeat = health.lastOrNull { it.optString("event") == "heartbeat" }?.optString("ts"),
            serviceStarts = health.count { it.optString("event") == "service_start" },
            sessionProblems = health.filter { it.optString("event") == "session" }.map { "${it.optString("ts").take(16)} ${it.optString("detail")}" },
        )
    }

    /** Intent para compartir los registros (ya enmascarados) con el menú de compartir de Android. */
    fun shareIntent(context: Context): Intent {
        val uris = arrayListOf(CALLS, HEALTH)
            .map { File(dir(context), it).apply { if (!exists()) createNewFile() } }
            .map { FileProvider.getUriForFile(context, "${context.packageName}.files", it) }
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, "WhatsCheck · registro de llamadas")
            putExtra(Intent.EXTRA_TEXT, summary(context).asText())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
