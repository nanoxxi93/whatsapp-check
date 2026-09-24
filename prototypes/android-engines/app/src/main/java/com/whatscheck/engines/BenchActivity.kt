package com.whatscheck.engines

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.widget.TextView
import com.whatscheck.engine.CheckResult
import com.whatscheck.engine.CheckStatus
import com.whatscheck.engines.whatsmeow.WhatsmeowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Una corrida del benchmark en frío (RF-05). La lanza `bench.sh` desde la PC después de `am force-stop`,
 * así cada corrida paga el arranque completo: proceso + motor + conexión + consulta.
 *
 *   adb shell am start -n com.whatscheck.engines/.BenchActivity --es engine whatsmeow --es phone <dígitos> --es network wifi
 *
 * Agrega una línea a files/results/AAAA-MM-DD.jsonl (número enmascarado) y termina el proceso.
 *
 * Con `--es action unlink` cierra la sesión del motor en WhatsApp y borra lo local (CA-09):
 *   adb shell am start -n com.whatscheck.engines/.BenchActivity --es engine whatsmeow --es action unlink
 */
class BenchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply { text = "Benchmark en curso…"; setPadding(48, 96, 48, 48) }
        setContentView(status)

        val engineId = intent.getStringExtra("engine") ?: "whatsmeow"
        val phone = intent.getStringExtra("phone").orEmpty().filter(Char::isDigit)
        val network = intent.getStringExtra("network")
        val engine = Engines.available(applicationContext).first { it.id == engineId }

        val action = intent.getStringExtra("action")
        if (action != null) {
            // Acciones de prueba por adb: unlink, hold (--es seconds 60) y stats. Resultado en actions.log, sin contenido.
            status.text = "Acción: $action…"
            CoroutineScope(Dispatchers.Main).launch {
                val outcome = runCatching {
                    when (action) {
                        "unlink" -> { engine.unlink(); "UNLINKED" }
                        "hold" -> {
                            val seconds = intent.getStringExtra("seconds")?.toLongOrNull() ?: 60L
                            "${seconds}s " + (engine as WhatsmeowEngine).hold(seconds)
                        }
                        "stats" -> (engine as WhatsmeowEngine).tableCounts()
                        // Solo muestra la notificación de sesión caída, para revisar cómo se ve (no toca la sesión)
                        "alert-test" -> {
                            com.whatscheck.engines.call.Notifier.sessionProblem(applicationContext, com.whatscheck.engine.SessionState.LOGGED_OUT)
                            "notificación de prueba mostrada"
                        }
                        else -> error("acción desconocida: $action")
                    }
                }.getOrElse { "ERROR ${it.message?.let(Phone::maskInText)}" }
                val dir = File(filesDir, "results").apply { mkdirs() }
                File(dir, "actions.log").appendText("${Instant.now()} $action ${engine.id}: $outcome\n")
                status.text = outcome
                finish()
                Process.killProcess(Process.myPid())
            }
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val t0 = SystemClock.elapsedRealtime()
            // Desde que el sistema creó el proceso hasta que empieza el trabajo del motor
            val tProcess = t0 - Process.getStartElapsedRealtime()
            var result: CheckResult = CheckResult.unknown(com.whatscheck.engine.UnknownReason.NOT_CONNECTED)
            var tEngine: Long? = null
            var tReady: Long? = null
            var tQuery: Long? = null
            var error: String? = null
            try {
                val timings = engine.connect()
                tEngine = timings.tEngineMs
                tReady = timings.tReadyMs
                val tq = SystemClock.elapsedRealtime()
                result = engine.check(phone)
                tQuery = SystemClock.elapsedRealtime() - tq
            } catch (e: Exception) {
                error = e.message?.let(Phone::maskInText)
            }
            val pssKb = Debug.getPss()
            runCatching { engine.close() }

            val record = JSONObject()
                .put("ts", Instant.now().toString())
                .put("kind", "bench-android")
                .put("engine", engine.id)
                .put("device", "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}")
                .put("network", network ?: JSONObject.NULL)
                .put("phone", Phone.mask(phone))
                .put("status", result.status.name)
                .put("reason", result.reason?.name ?: JSONObject.NULL)
                .put("jidMismatch", result.jidMismatch)
                .put("tProcess", tProcess)
                .put("tEngine", tEngine ?: JSONObject.NULL)
                .put("tReady", tReady ?: JSONObject.NULL)
                .put("tQuery", tQuery ?: JSONObject.NULL)
                // Mismo significado que en baileys-cli: motor + conexión + consulta, sin el arranque del proceso
                .put("tTotal", if (tReady != null && tQuery != null) tReady + tQuery else JSONObject.NULL)
                .put("pssKb", pssKb)
                .put("error", error ?: JSONObject.NULL)
            val dir = File(filesDir, "results").apply { mkdirs() }
            File(dir, "${LocalDate.now()}.jsonl").appendText(record.toString() + "\n")

            status.text = "${result.status}" + if (result.status == CheckStatus.UNKNOWN) " (${result.reason})" else ""
            finish()
            Process.killProcess(Process.myPid())
        }
    }
}
