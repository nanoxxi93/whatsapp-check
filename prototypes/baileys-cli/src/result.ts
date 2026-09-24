/**
 * Traducción de la respuesta de onWhatsApp() a CheckResult.
 * Contrato: specs/product/tech-stack.md → "Contrato de la consulta (onWhatsApp de Baileys)".
 * Es la referencia para implementar la misma lógica en Kotlin.
 */

export type CheckStatus = 'HAS_WHATSAPP' | 'NO_WHATSAPP' | 'UNKNOWN'

export type UnknownReason =
  | 'timeout' // la consulta no respondió a tiempo
  | 'error' // la consulta lanzó una excepción
  | 'session_closed' // la sesión no estaba abierta al terminar la consulta
  | 'no_response' // onWhatsApp devolvió undefined (la consulta USync no trajo resultado)
  | 'ambiguous' // varias entradas y ninguna coincide con el número enviado
  | 'not_connected' // no se llegó a consultar: la sesión no se pudo abrir (sin red, timeout de conexión)

export interface CheckResult {
  status: CheckStatus
  /** jid devuelto por WhatsApp, si lo hubo. */
  jid?: string
  /** true si se aceptó una única entrada cuyo jid no coincide exactamente con el número enviado. */
  jidMismatch: boolean
  reason?: UnknownReason
}

export interface OnWhatsAppEntry {
  jid: string
  exists: boolean
}

export type QueryOutcome =
  | { kind: 'response'; entries: OnWhatsAppEntry[] | undefined; sessionOpen: boolean }
  | { kind: 'error'; error: unknown }
  | { kind: 'timeout' }

const unknown = (reason: UnknownReason): CheckResult => ({ status: 'UNKNOWN', jidMismatch: false, reason })

/** "51987654321@s.whatsapp.net" o "51987654321:12@s.whatsapp.net" → "51987654321" */
export function jidUser(jid: string): string {
  return jid.split('@')[0]!.split(':')[0]!
}

export function toCheckResult(sentDigits: string, outcome: QueryOutcome): CheckResult {
  if (outcome.kind === 'timeout') return unknown('timeout')
  if (outcome.kind === 'error') return unknown('error')
  // Un [] solo significa "no tiene WhatsApp" si la sesión seguía abierta: fallar en silencio.
  if (!outcome.sessionOpen) return unknown('session_closed')
  if (outcome.entries === undefined) return unknown('no_response')

  const entries = outcome.entries
  if (entries.length === 0) return { status: 'NO_WHATSAPP', jidMismatch: false }

  const exact = entries.find((e) => jidUser(e.jid) === sentDigits)
  const match = exact ?? (entries.length === 1 ? entries[0] : undefined)
  if (!match) return unknown('ambiguous')

  return {
    status: match.exists ? 'HAS_WHATSAPP' : 'NO_WHATSAPP',
    jid: match.jid,
    jidMismatch: exact === undefined,
  }
}
