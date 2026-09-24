export interface Summary {
  n: number
  min: number
  p50: number
  p95: number
  max: number
}

/** Percentil por rango más cercano: el valor en la posición ceil(p/100 · n) de la lista ordenada. */
export function percentile(sorted: readonly number[], p: number): number {
  if (sorted.length === 0) throw new Error('percentile: lista vacía')
  const rank = Math.ceil((p / 100) * sorted.length)
  return sorted[Math.min(Math.max(rank, 1), sorted.length) - 1]!
}

export function summarize(values: readonly number[]): Summary | null {
  if (values.length === 0) return null
  const sorted = [...values].sort((a, b) => a - b)
  return {
    n: sorted.length,
    min: sorted[0]!,
    p50: percentile(sorted, 50),
    p95: percentile(sorted, 95),
    max: sorted[sorted.length - 1]!,
  }
}

export type Verdict = 'GO' | 'GO_CON_RESERVAS' | 'NO_GO'

/**
 * Criterios Go / No-Go de specs/2026-09-23-prototipo-baileys-escritorio/validation.md.
 * El peor de los dos criterios define el resultado.
 */
export function verdict(p95TotalMs: number, connectionFailures: number): Verdict {
  const byLatency: Verdict = p95TotalMs <= 3000 ? 'GO' : p95TotalMs <= 8000 ? 'GO_CON_RESERVAS' : 'NO_GO'
  const byFailures: Verdict = connectionFailures === 0 ? 'GO' : connectionFailures <= 2 ? 'GO_CON_RESERVAS' : 'NO_GO'
  const order: Verdict[] = ['GO', 'GO_CON_RESERVAS', 'NO_GO']
  return order[Math.max(order.indexOf(byLatency), order.indexOf(byFailures))]!
}
