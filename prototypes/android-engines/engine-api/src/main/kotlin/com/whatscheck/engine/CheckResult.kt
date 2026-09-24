package com.whatscheck.engine

/**
 * Traducción de la respuesta del motor a CheckResult.
 * Contrato: specs/product/tech-stack.md → "Contrato de la consulta (onWhatsApp de Baileys)".
 * Puerto de prototypes/baileys-cli/src/result.ts; los tests repiten los mismos casos.
 */

enum class CheckStatus { HAS_WHATSAPP, NO_WHATSAPP, UNKNOWN }

enum class UnknownReason {
    /** La consulta no respondió a tiempo. */
    TIMEOUT,

    /** La consulta lanzó una excepción. */
    ERROR,

    /** La sesión no estaba abierta al terminar la consulta. */
    SESSION_CLOSED,

    /** El motor devolvió una respuesta nula (en Baileys 7: la consulta USync no trajo resultado). */
    NO_RESPONSE,

    /** Varias entradas y ninguna coincide con el número enviado. */
    AMBIGUOUS,

    /** No se llegó a consultar: la sesión no se pudo abrir (sin red, timeout de conexión). */
    NOT_CONNECTED,
}

data class CheckResult(
    val status: CheckStatus,
    /** jid devuelto por WhatsApp, si lo hubo. */
    val jid: String? = null,
    /** true si se aceptó una única entrada cuyo jid no coincide exactamente con el número enviado. */
    val jidMismatch: Boolean = false,
    val reason: UnknownReason? = null,
) {
    companion object {
        fun unknown(reason: UnknownReason) = CheckResult(CheckStatus.UNKNOWN, reason = reason)
    }
}

/** Una entrada de la respuesta del motor, p. ej. `{ jid: "51987654321@s.whatsapp.net", exists: true }`. */
data class PresenceEntry(val jid: String, val exists: Boolean)

sealed interface QueryOutcome {
    data class Response(val entries: List<PresenceEntry>?, val sessionOpen: Boolean) : QueryOutcome
    data class Failure(val error: Throwable) : QueryOutcome
    data object Timeout : QueryOutcome
}

/** "51987654321@s.whatsapp.net" o "51987654321:12@s.whatsapp.net" → "51987654321" */
fun jidUser(jid: String): String = jid.substringBefore('@').substringBefore(':')

fun toCheckResult(sentDigits: String, outcome: QueryOutcome): CheckResult = when (outcome) {
    QueryOutcome.Timeout -> CheckResult.unknown(UnknownReason.TIMEOUT)
    is QueryOutcome.Failure -> CheckResult.unknown(UnknownReason.ERROR)
    is QueryOutcome.Response -> fromResponse(sentDigits, outcome)
}

private fun fromResponse(sentDigits: String, response: QueryOutcome.Response): CheckResult {
    // Una lista vacía solo significa "no tiene WhatsApp" si la sesión seguía abierta: fallar en silencio.
    if (!response.sessionOpen) return CheckResult.unknown(UnknownReason.SESSION_CLOSED)
    val entries = response.entries ?: return CheckResult.unknown(UnknownReason.NO_RESPONSE)
    if (entries.isEmpty()) return CheckResult(CheckStatus.NO_WHATSAPP)

    val exact = entries.firstOrNull { jidUser(it.jid) == sentDigits }
    val match = exact ?: entries.singleOrNull() ?: return CheckResult.unknown(UnknownReason.AMBIGUOUS)
    return CheckResult(
        status = if (match.exists) CheckStatus.HAS_WHATSAPP else CheckStatus.NO_WHATSAPP,
        jid = match.jid,
        jidMismatch = exact == null,
    )
}
