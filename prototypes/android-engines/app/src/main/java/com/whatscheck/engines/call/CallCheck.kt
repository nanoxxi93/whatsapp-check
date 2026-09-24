package com.whatscheck.engines.call

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.telephony.PhoneNumberUtils
import com.whatscheck.engine.CheckResult
import com.whatscheck.engine.CheckStatus
import com.whatscheck.engine.SessionState
import com.whatscheck.engine.UnknownReason
import com.whatscheck.engines.Engines
import com.whatscheck.engines.Phone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Verificación de una llamada entrante (RF-03, RF-06): normaliza el número, consulta con whatsmeow en frío
 * (conectar + consultar + cerrar) y deja registro enmascarado. Nunca lanza: los fallos son ❔.
 */
object CallCheck {
    /** Scope del proceso: la consulta sigue aunque el servicio de filtrado ya haya respondido. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()
    @Volatile private var overlay: OverlayController? = null

    fun overlay(context: Context): OverlayController =
        overlay ?: OverlayController(context.applicationContext).also { overlay = it }

    /** "+51 987 654 321", "987654321", "tel:…" → "51987654321"; null si está oculto o no es válido. */
    fun normalize(raw: String?, country: String = "PE"): String? {
        if (raw.isNullOrBlank()) return null
        val e164 = PhoneNumberUtils.formatNumberToE164(raw, country) ?: return null
        return e164.removePrefix("+").takeIf { it.length in 8..15 }
    }

    data class Outcome(
        val result: CheckResult,
        val detail: String?, // motivo legible si es ❔ (sin números completos)
        val tEngine: Long?,
        val tReady: Long?,
        val tQuery: Long?,
    )

    /** Consulta en frío. Se serializa: si entran dos llamadas seguidas, la segunda espera a la primera. */
    suspend fun verify(context: Context, digits: String?): Outcome = withContext(Dispatchers.IO) {
        if (digits == null) {
            return@withContext Outcome(CheckResult.unknown(UnknownReason.NOT_CONNECTED), "número oculto o no válido", null, null, null)
        }
        val engine = Engines.whatsmeow(context)
        mutex.withLock {
            try {
                if (!engine.isLinked()) {
                    return@withLock Outcome(CheckResult.unknown(UnknownReason.NOT_CONNECTED), "sin sesión vinculada", null, null, null)
                }
                val timings = engine.connect()
                val tq = SystemClock.elapsedRealtime()
                val result = engine.check(digits)
                val tQuery = SystemClock.elapsedRealtime() - tq
                Outcome(result, result.reason?.name, timings.tEngineMs, timings.tReadyMs, tQuery)
            } catch (e: Exception) {
                Outcome(CheckResult.unknown(UnknownReason.NOT_CONNECTED), e.message?.let(Phone::maskInText), null, null, null)
            } finally {
                runCatching { engine.close() }
                reportSession(context, engine.sessionState.value, engine.lastError())
            }
        }
    }

    /** Eventos de sesión que importan para la prueba de resistencia al baneo (RF-10). */
    private fun reportSession(context: Context, state: SessionState, lastError: String) {
        if (state == SessionState.LOGGED_OUT || state == SessionState.BANNED) {
            Journal.health(context, "session", "$state ${Phone.maskInText(lastError)}".trim())
        }
    }

    fun kindOf(result: CheckResult): OverlayController.Kind = when (result.status) {
        CheckStatus.HAS_WHATSAPP -> OverlayController.Kind.HAS
        CheckStatus.NO_WHATSAPP -> OverlayController.Kind.NO
        CheckStatus.UNKNOWN -> OverlayController.Kind.UNKNOWN
    }

    /** Una línea de calls.jsonl. Los tiempos se miden desde que entró la llamada (t0). */
    fun record(
        context: Context,
        digits: String?,
        outcome: Outcome,
        via: String,
        t0: Long,
        tScreen: Long,
        tOverlay: Long?,
        tResult: Long,
    ): JSONObject {
        val pm = context.getSystemService(PowerManager::class.java)
        val km = context.getSystemService(KeyguardManager::class.java)
        return JSONObject()
            .put("phone", digits?.let(Phone::mask) ?: JSONObject.NULL)
            .put("status", outcome.result.status.name)
            .put("reason", outcome.result.reason?.name ?: JSONObject.NULL)
            .put("detail", outcome.detail ?: JSONObject.NULL)
            .put("via", via)
            .put("tScreen", tScreen)
            .put("tOverlay", tOverlay ?: JSONObject.NULL)
            .put("tResult", tResult)
            .put("tEngine", outcome.tEngine ?: JSONObject.NULL)
            .put("tReady", outcome.tReady ?: JSONObject.NULL)
            .put("tQuery", outcome.tQuery ?: JSONObject.NULL)
            // Proceso recién creado para esta llamada (app cerrada) o ya vivo (servicio "siempre activo")
            .put("processColdStart", t0 - Process.getStartElapsedRealtime() < 3_000)
            .put("screenOn", pm.isInteractive)
            .put("locked", km.isKeyguardLocked)
    }
}
