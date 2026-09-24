import { readFile, rm } from 'node:fs/promises'
import { createInterface } from 'node:readline/promises'
import { parseArgs } from 'node:util'
import type { CountryCode } from 'libphonenumber-js'
import { queryNumber, randomPause } from './check.ts'
import { appendJsonl, fmtMs, printResults, printSummary, statusText, type ResultRecord } from './output.ts'
import { DEFAULT_COUNTRY, InvalidPhoneError, maskInText, maskPhone, toE164Digits } from './phone.ts'
import type { CheckResult } from './result.ts'
import {
  AlreadyLinkedError, BROWSERS, ConnectTimeoutError, DEFAULT_AUTH_DIR, DEFAULT_BROWSER, NotLinkedError,
  SessionClosedError, browserLabel, createLogger, isFatal, linkDevice, openSession, readStoredSession, setBrowser,
  unlinkDevice, type BrowserPreset, type Session,
} from './session.ts'
import { summarize, verdict } from './stats.ts'

const MAX_BATCH = 50 // RNF-01
const MAX_RUNS = 30 // RNF-01
const MIN_BENCH_PAUSE_S = 10 // RNF-01

const HELP = `
Prototipo WhatsCheck — Baileys (Fase 0). Usar SOLO con un número secundario.

Uso: npm start -- <comando> [opciones]

  link <número>                 Vincula el número con código de emparejamiento
  check <número…>               Consulta si los números tienen WhatsApp (conexión en frío)
  check --file <archivo>        Igual, leyendo "número[,etiqueta]" por línea (# = comentario)
  bench <número> [--runs N]     Repite N veces conexión en frío + consulta (por defecto 20, máx. ${MAX_RUNS})
  status                        Muestra si hay sesión guardada
  unlink                        Cierra la sesión en WhatsApp y borra las credenciales locales

Opciones:
  --max <N>        Permite lotes de más de ${MAX_BATCH} números (pide confirmación)
  --pause <s>      Pausa mínima entre corridas de bench (por defecto y mínimo ${MIN_BENCH_PAUSE_S} s)
  --network <txt>  Etiqueta de la red para los resultados (p. ej. "wifi-casa", "4g")
  --country <CC>   País por defecto para números sin código (por defecto ${DEFAULT_COUNTRY})
  --auth-dir <dir> Carpeta de credenciales (por defecto ${DEFAULT_AUTH_DIR})
  --browser <p>    Identidad al vincular: ${Object.keys(BROWSERS).join(' | ')} (por defecto ${DEFAULT_BROWSER})
  LOG_LEVEL=debug  Variable de entorno para ver los logs de Baileys (con números enmascarados)
`

class UsageError extends Error {}

async function ask(question: string): Promise<string> {
  const rl = createInterface({ input: process.stdin, output: process.stdout })
  try {
    return (await rl.question(question)).trim().toLowerCase()
  } finally {
    rl.close()
  }
}

const isYes = (answer: string) => ['si', 'sí', 's', 'yes', 'y'].includes(answer)

interface Target {
  digits: string
  label: string | null
}

async function readTargets(positionals: string[], file: string | undefined, country: CountryCode): Promise<Target[]> {
  const raw: { input: string; label: string | null }[] = positionals.map((input) => ({ input, label: null }))
  if (file) {
    for (const line of (await readFile(file, 'utf8')).split(/\r?\n/)) {
      const clean = line.replace(/#.*/, '').trim()
      if (!clean) continue
      const [input, label] = clean.split(',').map((s) => s.trim())
      raw.push({ input: input!, label: label || null })
    }
  }
  const targets: Target[] = []
  for (const { input, label } of raw) {
    try {
      targets.push({ digits: toE164Digits(input, country), label })
    } catch (e) {
      if (!(e instanceof InvalidPhoneError)) throw e
      console.warn(`⏭️  Se omite: ${e.message}`)
    }
  }
  return targets
}

function record(kind: ResultRecord['kind'], t: Target, result: CheckResult, extra: Partial<ResultRecord>): ResultRecord {
  return {
    ts: new Date().toISOString(),
    kind,
    phone: maskPhone(t.digits),
    label: t.label,
    status: result.status,
    reason: result.reason ?? null,
    jidMismatch: result.jidMismatch,
    cold: false,
    tWs: null,
    tReady: null,
    tQuery: null,
    tTotal: null,
    network: null,
    ...extra,
  }
}

/** Resultado para números que no se llegaron a consultar porque la sesión no abrió o se cayó. */
function notQueried(e: unknown): CheckResult {
  const fatal = e instanceof SessionClosedError && isFatal(e.reason)
  return { status: 'UNKNOWN', jidMismatch: false, reason: fatal ? 'session_closed' : 'not_connected' }
}

/** Abre la sesión; si falla, explica el motivo y devuelve el error en lugar de lanzar. */
async function tryOpen(authDir: string, logger: ReturnType<typeof createLogger>): Promise<Session | Error> {
  try {
    return await openSession({ authDir, logger })
  } catch (e) {
    if (e instanceof NotLinkedError) throw e
    if (e instanceof SessionClosedError || e instanceof ConnectTimeoutError) return e
    return e instanceof Error ? e : new Error(String(e))
  }
}

// ─── Comandos ────────────────────────────────────────────────────────────────

async function cmdLink(positionals: string[], authDir: string, country: CountryCode): Promise<number> {
  if (positionals.length !== 1) throw new UsageError('Uso: link <número>')
  const digits = toE164Digits(positionals[0]!, country)

  console.log(`\nVas a vincular ${maskPhone(digits)} como dispositivo de WhatsApp.`)
  console.log('Este prototipo usa un cliente NO oficial: la cuenta podría ser restringida o suspendida.')
  const answer = await ask('¿Es un número SECUNDARIO de pruebas y no tu cuenta personal? Escribe "si" para continuar: ')
  if (!isYes(answer)) {
    console.log('Cancelado. No se pidió ningún código.')
    return 1
  }

  console.log(`\nConectando con WhatsApp como "${browserLabel()}"…`)
  // Ctrl+C a mitad de la vinculación: no dejar credenciales a medias (IF-008)
  process.once('SIGINT', () => {
    void rm(authDir, { recursive: true, force: true }).finally(() => {
      console.log('\nVinculación cancelada; se borraron las credenciales incompletas.')
      process.exit(130)
    })
  })
  await linkDevice({
    authDir,
    logger: createLogger(),
    phoneDigits: digits,
    onCode: (code) => {
      const pretty = code.length === 8 ? `${code.slice(0, 4)}-${code.slice(4)}` : code
      console.log(`\n   Código de emparejamiento:  ${pretty}\n`)
      console.log('   En el teléfono del número secundario:')
      console.log('   1. Abre WhatsApp → Ajustes (o ⋮) → Dispositivos vinculados')
      console.log('   2. Toca "Vincular un dispositivo"')
      console.log('   3. Toca "Vincular con el número de teléfono" (abajo, en lugar de escanear el QR)')
      console.log('   4. Escribe el código de arriba\n')
      console.log('Esperando a que ingreses el código (máx. 3 min)…')
      console.log('Si el teléfono dice "No se pudo vincular el dispositivo", corta con Ctrl+C y prueba otro --browser.')
    },
    onProgress: (message) => console.log(message),
  })
  console.log(`\n✅ Vinculado. Credenciales guardadas en "${authDir}/". Aparecerá como "${browserLabel()}".`)
  return 0
}

async function cmdCheck(
  positionals: string[],
  opts: { file?: string; max?: string; network?: string },
  authDir: string,
  country: CountryCode,
): Promise<number> {
  const targets = await readTargets(positionals, opts.file, country)
  if (targets.length === 0) throw new UsageError('No hay números válidos para consultar.')

  if (targets.length > MAX_BATCH) {
    const allowed = opts.max ? Number(opts.max) : NaN
    if (!(allowed >= targets.length)) {
      throw new UsageError(`Son ${targets.length} números y el máximo por ejecución es ${MAX_BATCH}. Usa --max ${targets.length} para confirmarlo.`)
    }
    const answer = await ask(`Vas a consultar ${targets.length} números con la cuenta de prueba. ¿Seguro? Escribe "si": `)
    if (!isYes(answer)) return 1
  }

  const network = opts.network ?? null
  const logger = createLogger()
  console.log(`Conectando en frío para consultar ${targets.length} número(s)…`)
  const session = await tryOpen(authDir, logger)

  const records: ResultRecord[] = []
  if (session instanceof Error) {
    console.error(`❔ ${session.message}`)
    for (const t of targets) records.push(record('check', t, notQueried(session), { network }))
  } else {
    const { tWs, tReady } = session.timings
    console.log(`Sesión lista: socket ${fmtMs(tWs)} · sesión ${fmtMs(tReady)}\n`)
    let announced = false
    for (const [i, t] of targets.entries()) {
      const closed = session.closedError()
      if (closed) {
        // RF-09: la sesión se cerró a mitad del lote; no se sigue consultando
        if (!announced) console.error(`❔ ${closed.message}`)
        announced = true
        records.push(record('check', t, notQueried(closed), { network }))
        continue
      }
      const { result, tQuery } = await queryNumber(session, t.digits)
      const cold = i === 0
      records.push(record('check', t, result, { cold, tWs, tReady, tQuery, tTotal: cold ? tReady + tQuery : null, network }))
      console.log(`  ${maskPhone(t.digits)}  ${statusText(records.at(-1)!)}  (${fmtMs(tQuery)})`)
      if (i < targets.length - 1) await randomPause(3, 6)
    }
    await session.end()
  }

  console.log('')
  printResults(records)
  console.log(`\nResultados agregados a ${await appendJsonl(records)}`)
  return records.some((r) => r.reason === 'session_closed') ? 2 : 0
}

async function cmdBench(
  positionals: string[],
  opts: { runs?: string; pause?: string; network?: string },
  authDir: string,
  country: CountryCode,
): Promise<number> {
  if (positionals.length !== 1) throw new UsageError('Uso: bench <número> [--runs N]')
  const target: Target = { digits: toE164Digits(positionals[0]!, country), label: null }
  const runs = opts.runs ? Number(opts.runs) : 20
  if (!Number.isInteger(runs) || runs < 1 || runs > MAX_RUNS) throw new UsageError(`--runs debe ser un entero entre 1 y ${MAX_RUNS}.`)
  const pause = Math.max(opts.pause ? Number(opts.pause) : MIN_BENCH_PAUSE_S, MIN_BENCH_PAUSE_S)
  const network = opts.network ?? null
  const logger = createLogger()

  console.log(`Benchmark: ${runs} corridas de conexión en frío + consulta a ${maskPhone(target.digits)}, pausa ${pause}–${pause + 5} s.\n`)
  const records: ResultRecord[] = []
  let fatal: SessionClosedError | null = null

  for (let i = 1; i <= runs; i++) {
    const session = await tryOpen(authDir, logger)
    if (session instanceof Error) {
      records.push(record('bench', target, notQueried(session), { cold: true, network }))
      console.log(`  Corrida ${i}/${runs}: ❔ ${session.message}`)
      if (session instanceof SessionClosedError && isFatal(session.reason)) { fatal = session; break }
    } else {
      const { tWs, tReady } = session.timings
      const { result, tQuery } = await queryNumber(session, target.digits)
      await session.end()
      const tTotal = tReady + tQuery
      records.push(record('bench', target, result, { cold: true, tWs, tReady, tQuery, tTotal, network }))
      console.log(`  Corrida ${i}/${runs}: ${statusText(records.at(-1)!)} · sesión ${fmtMs(tReady)} · consulta ${fmtMs(tQuery)} · total ${fmtMs(tTotal)}`)
    }
    if (i < runs) await randomPause(pause, pause + 5)
  }

  const ok = records.filter((r) => r.status !== 'UNKNOWN')
  const values = (key: 'tWs' | 'tReady' | 'tQuery' | 'tTotal') =>
    ok.map((r) => r[key]).filter((v): v is number => v !== null)
  const failures = records.length - ok.length
  const total = summarize(values('tTotal'))

  console.log('')
  printSummary([
    ['Socket abierto (t_ws)', summarize(values('tWs'))],
    ['Sesión lista (t_ready)', summarize(values('tReady'))],
    ['Consulta (t_query)', summarize(values('tQuery'))],
    ['Total en frío (t_total)', total],
  ])
  console.log(`\nCorridas sin verificar (❔): ${failures} de ${records.length}`)
  if (total) {
    const labels = { GO: 'Go "bajo demanda"', GO_CON_RESERVAS: 'Go con reservas', NO_GO: 'No-Go "bajo demanda"' }
    console.log(`Veredicto según validation.md: ${labels[verdict(total.p95, failures)]} (p95 total ${fmtMs(total.p95)})`)
    if (records.length < 20) console.log('Nota: los criterios Go/No-Go están definidos para 20 corridas.')
  }
  console.log(`\nResultados agregados a ${await appendJsonl(records)}`)
  if (fatal) console.error(`\n${fatal.message}`)
  return fatal ? 2 : 0
}

async function cmdStatus(authDir: string): Promise<number> {
  const stored = await readStoredSession(authDir)
  if (!stored?.registered) {
    console.log(`Sin sesión vinculada en "${authDir}/".`)
    return 1
  }
  const phone = stored.meId ? maskPhone(stored.meId.split(/[:@]/)[0]!) : 'desconocido'
  console.log(`Sesión vinculada: ${phone} (credenciales en "${authDir}/").`)
  console.log('Esto no comprueba la conexión: si la cerraste desde el teléfono, "check" lo dirá.')
  return 0
}

async function cmdUnlink(authDir: string): Promise<number> {
  const stored = await readStoredSession(authDir)
  if (!stored) {
    console.log(`No hay credenciales en "${authDir}/".`)
    return 1
  }
  const answer = await ask('Se cerrará la sesión en WhatsApp y se borrarán las credenciales locales. Escribe "si": ')
  if (!isYes(answer)) return 1
  const result = await unlinkDevice({ authDir, logger: createLogger() })
  console.log(result === 'logged_out'
    ? '✅ Sesión cerrada en WhatsApp y credenciales borradas.'
    : '✅ La sesión ya estaba cerrada desde el teléfono; se borraron las credenciales locales.')
  return 0
}

// ─── Entrada ─────────────────────────────────────────────────────────────────

async function main(argv: string[]): Promise<number> {
  const { values, positionals } = parseArgs({
    args: argv,
    allowPositionals: true,
    options: {
      file: { type: 'string' },
      max: { type: 'string' },
      runs: { type: 'string' },
      pause: { type: 'string' },
      network: { type: 'string' },
      country: { type: 'string', default: DEFAULT_COUNTRY },
      'auth-dir': { type: 'string', default: DEFAULT_AUTH_DIR },
      browser: { type: 'string', default: DEFAULT_BROWSER },
      help: { type: 'boolean', short: 'h' },
    },
  })
  const [command, ...args] = positionals
  if (!(values.browser! in BROWSERS)) throw new UsageError(`--browser debe ser uno de: ${Object.keys(BROWSERS).join(', ')}`)
  setBrowser(values.browser as BrowserPreset)
  const authDir = values['auth-dir']!
  const country = values.country!.toUpperCase() as CountryCode

  switch (command) {
    case 'link': return cmdLink(args, authDir, country)
    case 'check': return cmdCheck(args, values, authDir, country)
    case 'bench': return cmdBench(args, values, authDir, country)
    case 'status': return cmdStatus(authDir)
    case 'unlink': return cmdUnlink(authDir)
    default:
      console.log(HELP)
      return command === undefined || values.help ? 0 : 1
  }
}

main(process.argv.slice(2)).then(
  (code) => process.exit(code),
  (e: unknown) => {
    if (e instanceof UsageError || e instanceof InvalidPhoneError || e instanceof NotLinkedError || e instanceof AlreadyLinkedError) {
      console.error(e.message)
      process.exit(1)
    }
    if (e instanceof SessionClosedError || e instanceof ConnectTimeoutError) {
      console.error(`❔ ${e.message}`)
      process.exit(2)
    }
    console.error(maskInText(e instanceof Error ? (e.stack ?? e.message) : String(e)))
    process.exit(1)
  },
)
