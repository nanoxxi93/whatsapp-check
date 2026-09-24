package com.whatscheck.engines.whatsmeow

import android.os.SystemClock
import com.whatscheck.engine.CheckResult
import com.whatscheck.engine.ConnectTimings
import com.whatscheck.engine.PresenceEntry
import com.whatscheck.engine.QueryOutcome
import com.whatscheck.engine.SessionState
import com.whatscheck.engine.UnknownReason
import com.whatscheck.engine.WhatsAppPresenceEngine
import com.whatscheck.engine.toCheckResult
import com.whatscheck.gen.wacheck.Engine
import com.whatscheck.gen.wacheck.Wacheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * F4: whatsmeow (Go) compilado con gomobile. La sesión vive en un SQLite dentro de [dataDir]
 * (filesDir de la app, fuera del backup: RNF-02). Las llamadas a Go bloquean, así que van en Dispatchers.IO.
 */
class WhatsmeowEngine(private val dataDir: File) : WhatsAppPresenceEngine {
    override val id = "whatsmeow"

    private val state = MutableStateFlow(SessionState.UNLINKED)
    override val sessionState: StateFlow<SessionState> = state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: Engine? = null

    /** Crea el motor si no existe: carga libgojni.so y abre el store. Es lo que mide tEngine. */
    private fun engine(): Engine = engine ?: Wacheck.newEngine(dataDir.absolutePath).also {
        engine = it
        state.value = parseState(it.state())
    }

    override suspend fun isLinked(): Boolean = withContext(Dispatchers.IO) { engine().isLinked() }

    override suspend fun requestPairingCode(phoneE164Digits: String): String = withContext(Dispatchers.IO) {
        val e = engine()
        state.value = SessionState.CONNECTING
        val code = e.requestPairingCode(phoneE164Digits, CONNECT_TIMEOUT_MS)
        // El usuario escribe el código en el teléfono; el resultado llega como cambio de estado en Go.
        scope.launch {
            val deadline = SystemClock.elapsedRealtime() + LINK_TIMEOUT_MS
            while (SystemClock.elapsedRealtime() < deadline) {
                val s = parseState(e.state())
                state.value = s
                if (s == SessionState.READY || s == SessionState.LOGGED_OUT || s == SessionState.BANNED) break
                delay(500)
            }
        }
        code
    }

    override suspend fun connect(): ConnectTimings = withContext(Dispatchers.IO) {
        val t0 = SystemClock.elapsedRealtime()
        val e = engine()
        val tEngine = SystemClock.elapsedRealtime() - t0
        state.value = SessionState.CONNECTING
        try {
            e.connect(CONNECT_TIMEOUT_MS)
        } finally {
            state.value = parseState(e.state())
        }
        ConnectTimings(tEngineMs = tEngine, tReadyMs = SystemClock.elapsedRealtime() - t0)
    }

    override suspend fun check(phoneE164Digits: String): CheckResult = withContext(Dispatchers.IO) {
        val e = engine ?: return@withContext CheckResult.unknown(UnknownReason.NOT_CONNECTED)
        if (!e.isConnected()) return@withContext CheckResult.unknown(UnknownReason.NOT_CONNECTED)
        val outcome = try {
            val entries = parseEntries(e.check(phoneE164Digits, QUERY_TIMEOUT_MS))
            QueryOutcome.Response(entries, sessionOpen = e.isConnected())
        } catch (ex: Exception) {
            if (ex.message.orEmpty().startsWith("timeout")) QueryOutcome.Timeout else QueryOutcome.Failure(ex)
        }
        toCheckResult(phoneE164Digits, outcome)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        engine?.close()
        engine?.let { state.value = parseState(it.state()) }
        Unit
    }

    override suspend fun unlink() = withContext(Dispatchers.IO) {
        engine().unlink(CONNECT_TIMEOUT_MS)
        engine?.release()
        engine = null
        state.value = SessionState.UNLINKED
    }

    /** Solo para pruebas: mantiene la sesión abierta [seconds] s. Devuelve JSON con eventos por tipo, sin contenido. */
    suspend fun hold(seconds: Long): String = withContext(Dispatchers.IO) {
        val e = engine()
        state.value = SessionState.CONNECTING
        try {
            e.hold(seconds)
        } finally {
            state.value = parseState(e.state())
        }
    }

    /** Último error informado por whatsmeow (vacío si no hubo); puede contener un jid: enmascarar antes de guardar. */
    fun lastError(): String = engine?.lastError().orEmpty()

    /** Solo para pruebas: filas por tabla de la base de whatsmeow, sin contenido. */
    suspend fun tableCounts(): String = withContext(Dispatchers.IO) { engine().tableCounts() }

    companion object {
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val QUERY_TIMEOUT_MS = 5_000L
        const val LINK_TIMEOUT_MS = 180_000L

        fun parseState(s: String): SessionState = runCatching { SessionState.valueOf(s) }.getOrDefault(SessionState.CLOSED)

        /** JSON de Go → entradas del contrato. "null" (sin resultado) → null → UNKNOWN(NO_RESPONSE). */
        fun parseEntries(json: String): List<PresenceEntry>? {
            if (json == "null") return null
            val array = JSONArray(json)
            return List(array.length()) { i ->
                val o = array.getJSONObject(i)
                PresenceEntry(jid = o.getString("jid"), exists = o.getBoolean("exists"))
            }
        }
    }
}
