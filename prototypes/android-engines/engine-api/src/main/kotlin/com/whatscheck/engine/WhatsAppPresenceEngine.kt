package com.whatscheck.engine

import kotlinx.coroutines.flow.StateFlow

enum class SessionState { UNLINKED, CONNECTING, READY, LOGGED_OUT, BANNED, CLOSED }

/** Tiempos de una conexión en frío, en milisegundos (RF-05 de la spec de comparación de motores). */
data class ConnectTimings(
    /** Desde el inicio hasta que el motor (runtime + librería + sesión cargada) está listo para conectar. */
    val tEngineMs: Long,
    /** Desde el inicio hasta que la sesión está abierta y se puede consultar. */
    val tReadyMs: Long,
)

/**
 * Motor que consulta si un número tiene WhatsApp usando la sesión vinculada del usuario.
 * Contrato: specs/product/tech-stack.md → "Contrato de la consulta". Las implementaciones
 * (whatsmeow, Baileys) deben aplicar la configuración obligatoria de la sesión (tech-stack §2).
 */
interface WhatsAppPresenceEngine {
    /** Nombre corto para mostrar y para los resultados, p. ej. "whatsmeow". */
    val id: String

    val sessionState: StateFlow<SessionState>

    /** true si hay credenciales de un emparejamiento completo guardadas. */
    suspend fun isLinked(): Boolean

    /**
     * Vincula con código de emparejamiento. Devuelve el código de 8 caracteres apenas WhatsApp lo entrega;
     * la vinculación termina cuando [sessionState] pasa a READY.
     */
    suspend fun requestPairingCode(phoneE164Digits: String): String

    /** Abre la sesión con las credenciales guardadas (conexión en frío). */
    suspend fun connect(): ConnectTimings

    /** Consulta un número (E.164 sin '+'). Nunca lanza: los fallos se devuelven como UNKNOWN. */
    suspend fun check(phoneE164Digits: String): CheckResult

    /** Cierra la conexión sin cerrar la sesión en WhatsApp. */
    suspend fun close()

    /** Cierra la sesión en WhatsApp (el dispositivo desaparece de "Dispositivos vinculados") y borra las credenciales. */
    suspend fun unlink()
}
