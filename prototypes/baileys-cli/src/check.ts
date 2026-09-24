import { performance } from 'node:perf_hooks'
import { toCheckResult, type CheckResult, type QueryOutcome } from './result.ts'
import type { Session } from './session.ts'

export const QUERY_TIMEOUT_MS = 5_000

const TIMEOUT = Symbol('timeout')

function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T | typeof TIMEOUT> {
  let timer: NodeJS.Timeout
  const timeout = new Promise<typeof TIMEOUT>((resolve) => { timer = setTimeout(() => resolve(TIMEOUT), ms) })
  promise.catch(() => {}) // si pierde la carrera y luego falla, que no sea un rechazo no manejado
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer))
}

/** Consulta un número con timeout (RNF-05) y lo traduce a CheckResult (RF-04). Nunca lanza. */
export async function queryNumber(
  session: Session,
  digits: string,
  timeoutMs = QUERY_TIMEOUT_MS,
): Promise<{ result: CheckResult; tQuery: number }> {
  const t0 = performance.now()
  let outcome: QueryOutcome
  if (!session.isOpen()) {
    outcome = { kind: 'response', entries: undefined, sessionOpen: false }
  } else {
    try {
      const entries = await withTimeout(session.sock.onWhatsApp(digits), timeoutMs)
      outcome = entries === TIMEOUT
        ? { kind: 'timeout' }
        : { kind: 'response', entries, sessionOpen: session.isOpen() }
    } catch (error) {
      outcome = { kind: 'error', error }
    }
  }
  return { result: toCheckResult(digits, outcome), tQuery: performance.now() - t0 }
}

/** Pausa aleatoria entre min y max segundos (RNF-01). */
export function randomPause(minSeconds: number, maxSeconds: number): Promise<void> {
  const ms = (minSeconds + Math.random() * (maxSeconds - minSeconds)) * 1000
  return new Promise((resolve) => setTimeout(resolve, ms))
}
