import { existsSync } from 'node:fs'
import { readFile, rm } from 'node:fs/promises'
import { join } from 'node:path'
import { performance } from 'node:perf_hooks'
import makeWASocket, {
  Browsers,
  useMultiFileAuthState,
  type AuthenticationState,
  type ConnectionState,
  type WASocket,
} from '@whiskeysockets/baileys'
import pino, { type Logger } from 'pino'
import { maskInText } from './phone.ts'

export const DEFAULT_AUTH_DIR = '.auth'
export const CONNECT_TIMEOUT_MS = 15_000
export const LINK_TIMEOUT_MS = 180_000

/**
 * Identidad del dispositivo vinculado: [sistema operativo, navegador, versión].
 * En "Dispositivos vinculados" se ve como "<navegador> (<sistema operativo>)".
 * 'macos' es el valor por defecto de Baileys y el más probado.
 * 'whatscheck' usa un nombre propio en la posición del sistema operativo, formato que se muestra bien al vincular por QR.
 * Con ['WhatsCheck', 'Chrome', '0.1.0'] el teléfono respondió "No se pudo vincular el dispositivo" (IF-009).
 */
export const BROWSERS = {
  macos: Browsers.macOS('Chrome'),
  ubuntu: Browsers.ubuntu('Chrome'),
  windows: Browsers.windows('Chrome'),
  whatscheck: ['WhatsCheck', 'Google Chrome', '1.0.0'],
} satisfies Record<string, [string, string, string]>

export type BrowserPreset = keyof typeof BROWSERS
export const DEFAULT_BROWSER: BrowserPreset = 'macos'
let browser: [string, string, string] = BROWSERS[DEFAULT_BROWSER]

/** Solo importa al vincular: las conexiones siguientes reutilizan la identidad registrada. */
export function setBrowser(preset: BrowserPreset): void {
  browser = BROWSERS[preset]
}

export const browserLabel = () => `${browser[1]} (${browser[0]})`

// ─── Motivos de cierre ────────────────────────────────────────────────────────

export type CloseReason =
  | 'logged_out' // 401: cerraron la sesión desde el teléfono
  | 'forbidden' // 403: cuenta restringida o bloqueada
  | 'replaced' // 440: otra conexión con las mismas credenciales
  | 'bad_session' // 500: credenciales corruptas
  | 'restart_required' // 515: normal justo después de vincular
  | 'connection' // 408 / 428 / 503: problemas de red o del servicio
  | 'unknown'

export function closeReason(statusCode: number | undefined): CloseReason {
  switch (statusCode) {
    case 401: return 'logged_out'
    case 403: return 'forbidden'
    case 440: return 'replaced'
    case 500: return 'bad_session'
    case 515: return 'restart_required'
    case 408: case 428: case 503: return 'connection'
    default: return 'unknown'
  }
}

/** Motivos que invalidan la sesión: no tiene sentido seguir consultando (RF-09). */
export function isFatal(reason: CloseReason): boolean {
  return reason === 'logged_out' || reason === 'forbidden' || reason === 'replaced' || reason === 'bad_session'
}

export function describeClose(reason: CloseReason, code?: number): string {
  const suffix = code === undefined ? '' : ` (código ${code})`
  switch (reason) {
    case 'logged_out': return `La sesión fue cerrada desde el teléfono; se borraron las credenciales locales. Vuelve a vincular con "link"${suffix}.`
    case 'forbidden': return `WhatsApp rechazó la sesión: la cuenta podría estar restringida. Detén las pruebas y revisa el teléfono${suffix}.`
    case 'replaced': return `Otra conexión está usando estas credenciales${suffix}.`
    case 'bad_session': return `Credenciales dañadas. Ejecuta "unlink" y vuelve a vincular${suffix}.`
    case 'restart_required': return `WhatsApp pidió reiniciar la conexión${suffix}.`
    case 'connection': return `Se perdió la conexión con WhatsApp${suffix}.`
    case 'unknown': return `La conexión se cerró por un motivo desconocido${suffix}.`
  }
}

export class SessionClosedError extends Error {
  constructor(readonly reason: CloseReason, readonly code?: number) {
    super(describeClose(reason, code))
    this.name = 'SessionClosedError'
  }
}

export class ConnectTimeoutError extends Error {
  constructor(ms: number) {
    super(`No se pudo conectar con WhatsApp en ${ms / 1000} s.`)
    this.name = 'ConnectTimeoutError'
  }
}

export class NotLinkedError extends Error {
  constructor() {
    super('No hay ninguna sesión vinculada. Ejecuta primero: npm start -- link <número>')
    this.name = 'NotLinkedError'
  }
}

export class AlreadyLinkedError extends Error {
  constructor() {
    super('Ya hay una sesión vinculada. Usa "unlink" antes de vincular otro número.')
    this.name = 'AlreadyLinkedError'
  }
}

const toClosedError = (error: Error | undefined): SessionClosedError => {
  const code = (error as { output?: { statusCode?: number } } | undefined)?.output?.statusCode
  return new SessionClosedError(closeReason(code), code)
}

// ─── Logger ──────────────────────────────────────────────────────────────────

/** Logger de Baileys: nivel warn por defecto y todos los números enmascarados (RNF-02). */
export function createLogger(level = process.env.LOG_LEVEL ?? 'warn'): Logger {
  return pino(
    { level, timestamp: pino.stdTimeFunctions.isoTime },
    { write: (line: string) => void process.stderr.write(maskInText(line)) },
  )
}

// ─── Socket ──────────────────────────────────────────────────────────────────

interface SocketHandle {
  sock: WASocket
  /** Promesas de guardado de credenciales pendientes; se esperan antes de salir. */
  flush: () => Promise<void>
}

/**
 * Configuración obligatoria de la sesión (tech-stack §2, RF-08):
 * no se marca "en línea" (Baileys envía presencia 'unavailable' y recibos 'inactive', no de lectura),
 * no se sincroniza historial, y no hay manejador de mensajes: lo que llega se descarta.
 * En este código nunca se llama a readMessages ni a sendPresenceUpdate.
 */
function createSocket(auth: AuthenticationState, saveCreds: () => Promise<void>, logger: Logger): SocketHandle {
  const sock = makeWASocket({
    auth,
    logger,
    browser,
    markOnlineOnConnect: false,
    syncFullHistory: false,
    shouldSyncHistoryMessage: () => false,
    connectTimeoutMs: CONNECT_TIMEOUT_MS,
  })
  const pending = new Set<Promise<void>>()
  sock.ev.on('creds.update', () => {
    const p = saveCreds().finally(() => pending.delete(p))
    pending.add(p)
  })
  return { sock, flush: async () => { await Promise.allSettled([...pending]) } }
}

/**
 * Cierra el socket y espera (máx. 2 s) a que el WebSocket termine de cerrarse. Si el proceso sale
 * mientras se cierra, Node en Windows puede abortar con "Assertion failed: !(handle->flags & UV_HANDLE_CLOSING)".
 */
async function closeSocket(sock: WASocket): Promise<void> {
  // sock.end() de Baileys ya espera el 'close' del WebSocket (y quita los listeners de 'close' del cliente).
  await Promise.race([
    Promise.resolve(sock.end(undefined)).catch(() => {}),
    new Promise((resolve) => setTimeout(resolve, 2_000)),
  ])
}

function waitForOpen(sock: WASocket, timeoutMs: number): Promise<void> {
  return new Promise((resolve, reject) => {
    const cleanup = () => {
      clearTimeout(timer)
      sock.ev.off('connection.update', onUpdate)
    }
    const timer = setTimeout(() => { cleanup(); reject(new ConnectTimeoutError(timeoutMs)) }, timeoutMs)
    const onUpdate = (u: Partial<ConnectionState>) => {
      if (u.connection === 'open') { cleanup(); resolve() }
      else if (u.connection === 'close') { cleanup(); reject(toClosedError(u.lastDisconnect?.error)) }
    }
    sock.ev.on('connection.update', onUpdate)
  })
}

const credsPath = (authDir: string) => join(authDir, 'creds.json')

/**
 * Tras un 401 (sesión cerrada desde el teléfono) las credenciales ya no sirven: se borran para que
 * "link" pueda vincular de nuevo sin pasar por "unlink". Con 403 se conservan para poder diagnosticar.
 */
async function forgetSession(authDir: string): Promise<void> {
  await rm(authDir, { recursive: true, force: true })
}

/**
 * Emparejamiento completo. `registered` lo pone Baileys cuando el teléfono acepta el código,
 * pero `account` solo llega con pair-success: sin él, las credenciales no sirven para iniciar sesión.
 */
const isPaired = (creds: { registered?: boolean; account?: unknown }) => creds.registered === true && creds.account != null

// ─── Sesión abierta ──────────────────────────────────────────────────────────

export interface Timings {
  /** ms desde el inicio hasta que el WebSocket quedó abierto. */
  tWs: number | null
  /** ms desde el inicio hasta connection: 'open' (sesión lista para consultar). */
  tReady: number
}

export interface Session {
  sock: WASocket
  timings: Timings
  isOpen(): boolean
  /** Motivo de cierre si la sesión se cayó después de abrirse. */
  closedError(): SessionClosedError | null
  /** Cierra la conexión sin cerrar la sesión en WhatsApp y guarda las credenciales. */
  end(): Promise<void>
}

/** Conexión en frío con credenciales guardadas. Mide t_ws y t_ready (RF-06). */
export async function openSession(opts: { authDir: string; logger: Logger; timeoutMs?: number }): Promise<Session> {
  const t0 = performance.now()
  if (!existsSync(credsPath(opts.authDir))) throw new NotLinkedError()
  const { state, saveCreds } = await useMultiFileAuthState(opts.authDir)
  if (!isPaired(state.creds)) throw new NotLinkedError()

  const { sock, flush } = createSocket(state, saveCreds, opts.logger)
  let tWs: number | null = null
  sock.ws.once('open', () => { tWs = performance.now() - t0 })

  try {
    await waitForOpen(sock, opts.timeoutMs ?? CONNECT_TIMEOUT_MS)
  } catch (e) {
    await closeSocket(sock)
    await flush()
    if (e instanceof SessionClosedError && e.reason === 'logged_out') await forgetSession(opts.authDir)
    throw e
  }
  const tReady = performance.now() - t0

  let open = true
  let closed: SessionClosedError | null = null
  let ending = false
  sock.ev.on('connection.update', (u) => {
    if (u.connection === 'close') {
      open = false
      if (!ending) closed = toClosedError(u.lastDisconnect?.error)
    }
  })

  return {
    sock,
    timings: { tWs, tReady },
    isOpen: () => open,
    closedError: () => closed,
    end: async () => {
      ending = true
      open = false
      await closeSocket(sock)
      await flush()
      if (closed?.reason === 'logged_out') await forgetSession(opts.authDir)
    },
  }
}

// ─── Vinculación ─────────────────────────────────────────────────────────────

/**
 * Vincula con código de emparejamiento (RF-01). Flujo de Baileys:
 * 1. Conectar sin credenciales → llega un 'qr' en connection.update: ahí se pide el código.
 * 2. El usuario ingresa el código en el teléfono → WhatsApp cierra con 515 (restart_required).
 * 3. Reconectar con las credenciales nuevas → connection: 'open'.
 */
export async function linkDevice(opts: {
  authDir: string
  logger: Logger
  phoneDigits: string
  onCode: (code: string) => void
  onProgress?: (message: string) => void
  timeoutMs?: number
}): Promise<void> {
  const stored = await readStoredSession(opts.authDir)
  if (stored?.registered) throw new AlreadyLinkedError()
  // Credenciales de un intento anterior que no terminó: requestPairingCode ya guardó creds.me,
  // y con creds.me Baileys intenta iniciar sesión en vez de registrarse → WhatsApp responde 401 (IF-008).
  if (stored) await rm(opts.authDir, { recursive: true, force: true })

  try {
    await pairAndConnect(opts)
  } catch (e) {
    await rm(opts.authDir, { recursive: true, force: true }) // no dejar credenciales a medias
    throw e
  }
}

async function pairAndConnect(opts: Parameters<typeof linkDevice>[0]): Promise<void> {
  const deadline = Date.now() + (opts.timeoutMs ?? LINK_TIMEOUT_MS)
  const { state, saveCreds } = await useMultiFileAuthState(opts.authDir)

  let handle = createSocket(state, saveCreds, opts.logger)
  const firstStep = await new Promise<'open' | 'restart'>((resolve, reject) => {
    let requested = false
    const timer = setTimeout(() => reject(new ConnectTimeoutError(opts.timeoutMs ?? LINK_TIMEOUT_MS)), deadline - Date.now())
    handle.sock.ev.on('connection.update', (u) => {
      if (u.qr && !requested) {
        requested = true
        handle.sock.requestPairingCode(opts.phoneDigits).then(opts.onCode, (e) => { clearTimeout(timer); reject(e) })
      }
      if (u.isNewLogin) opts.onProgress?.('Código aceptado por WhatsApp.')
      if (u.connection === 'open') { clearTimeout(timer); resolve('open') }
      if (u.connection === 'close') {
        clearTimeout(timer)
        const err = toClosedError(u.lastDisconnect?.error)
        if (err.reason === 'restart_required') resolve('restart')
        else reject(err)
      }
    })
  }).catch(async (e) => {
    await closeSocket(handle.sock)
    await handle.flush()
    throw e
  })

  if (firstStep === 'restart') {
    opts.onProgress?.('Reconectando con las credenciales nuevas…')
    await handle.flush()
    handle = createSocket(state, saveCreds, opts.logger)
    try {
      await waitForOpen(handle.sock, Math.max(deadline - Date.now(), CONNECT_TIMEOUT_MS))
    } catch (e) {
      await closeSocket(handle.sock)
      await handle.flush()
      throw e
    }
  }
  await closeSocket(handle.sock)
  await handle.flush()
}

// ─── Estado y desvinculación ─────────────────────────────────────────────────

export interface StoredSession {
  /** true solo si el emparejamiento se completó (ver isPaired). */
  registered: boolean
  /** jid propio, p. ej. "51987654321:12@s.whatsapp.net". */
  meId?: string
}

/** Lee las credenciales guardadas sin conectarse ni crear la carpeta. */
export async function readStoredSession(authDir: string): Promise<StoredSession | null> {
  if (!existsSync(credsPath(authDir))) return null
  const creds = JSON.parse(await readFile(credsPath(authDir), 'utf8')) as { registered?: boolean; account?: unknown; me?: { id?: string } }
  return { registered: isPaired(creds), meId: creds.me?.id }
}

/**
 * Cierra la sesión en WhatsApp (el dispositivo desaparece de "Dispositivos vinculados") y borra las credenciales.
 * Si la sesión ya estaba cerrada desde el teléfono, solo borra lo local. Devuelve qué pasó.
 */
export async function unlinkDevice(opts: { authDir: string; logger: Logger }): Promise<'logged_out' | 'local_only'> {
  let result: 'logged_out' | 'local_only' = 'local_only'
  try {
    const session = await openSession(opts)
    try {
      await session.sock.logout()
      result = 'logged_out'
    } finally {
      await session.end()
    }
  } catch (e) {
    if (!(e instanceof SessionClosedError && isFatal(e.reason))) throw e
  }
  await rm(opts.authDir, { recursive: true, force: true })
  return result
}
