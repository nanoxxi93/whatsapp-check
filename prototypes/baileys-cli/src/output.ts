import { appendFile, mkdir } from 'node:fs/promises'
import { join } from 'node:path'
import type { CheckStatus, UnknownReason } from './result.ts'
import type { Summary } from './stats.ts'

export const DEFAULT_RESULTS_DIR = 'results'

/** Una línea del JSONL (RF-11). Los tiempos van en milisegundos. */
export interface ResultRecord {
  ts: string
  kind: 'check' | 'bench'
  /** Número enmascarado; nunca el número completo (RNF-02). */
  phone: string
  /** Etiqueta opcional del archivo de entrada (p. ej. "spam" / "legitimo"), para la validación de la hipótesis. */
  label: string | null
  status: CheckStatus
  reason: UnknownReason | null
  jidMismatch: boolean
  /** true en la primera consulta tras conectar en frío; en ella t_total = t_ready + t_query. */
  cold: boolean
  tWs: number | null
  tReady: number | null
  tQuery: number | null
  tTotal: number | null
  network: string | null
}

const ICONS: Record<CheckStatus, string> = {
  HAS_WHATSAPP: '✅ tiene WhatsApp',
  NO_WHATSAPP: '⚠️  no tiene WhatsApp',
  UNKNOWN: '❔ no verificado',
}

export const fmtMs = (ms: number | null): string => (ms === null ? '—' : `${(ms / 1000).toFixed(2)} s`)

export function statusText(r: Pick<ResultRecord, 'status' | 'reason' | 'jidMismatch'>): string {
  const extra = [r.reason, r.jidMismatch ? 'jid distinto' : null].filter(Boolean).join(', ')
  return extra ? `${ICONS[r.status]} (${extra})` : ICONS[r.status]
}

function printAligned(header: string[], rows: string[][]): void {
  const widths = header.map((h, i) => Math.max(h.length, ...rows.map((r) => r[i]!.length)))
  const line = (cells: string[]) => cells.map((c, i) => c.padEnd(widths[i]!)).join('  ')
  console.log(line(header))
  console.log(widths.map((w) => '─'.repeat(w)).join('  '))
  rows.forEach((r) => console.log(line(r)))
}

export function printResults(records: ResultRecord[]): void {
  printAligned(
    ['Número', 'Etiqueta', 'Resultado', 'Consulta', 'Total en frío'],
    records.map((r) => [r.phone, r.label ?? '', statusText(r), fmtMs(r.tQuery), r.cold ? fmtMs(r.tTotal) : '']),
  )
}

export function printSummary(rows: [string, Summary | null][]): void {
  printAligned(
    ['Fase', 'n', 'mín', 'p50', 'p95', 'máx'],
    rows.map(([name, s]) => s
      ? [name, String(s.n), fmtMs(s.min), fmtMs(s.p50), fmtMs(s.p95), fmtMs(s.max)]
      : [name, '0', '—', '—', '—', '—']),
  )
}

/** Fecha local AAAA-MM-DD (la misma que `date +%F`); toISOString() daría la fecha UTC. */
export const localDate = (d = new Date()): string =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

/** Agrega los registros a results/AAAA-MM-DD.jsonl (fecha local) y devuelve la ruta. */
export async function appendJsonl(records: ResultRecord[], dir = DEFAULT_RESULTS_DIR): Promise<string> {
  await mkdir(dir, { recursive: true })
  const file = join(dir, `${localDate()}.jsonl`)
  await appendFile(file, records.map((r) => JSON.stringify(r)).join('\n') + '\n', 'utf8')
  return file
}
