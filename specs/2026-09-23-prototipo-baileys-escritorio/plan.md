# Plan — Prototipo de escritorio con Baileys

> **Estado:** Validado · **Fecha:** 2026-09-23
> **Relacionado:** [spec.md](spec.md) · [checklist.md](checklist.md) · [tech-stack](../product/tech-stack.md)

## Enfoque

Una CLI en TypeScript que se ejecuta con `tsx`, sin paso de compilación, en `prototypes/baileys-cli/`. La lógica que después se replicará en Kotlin se escribe como **funciones puras con tests**: la traducción de la respuesta a `CheckResult`, la coincidencia por `jid`, la normalización y el enmascarado de números. Así sirve de especificación ejecutable para el motor de Android. Lo que depende de la red (vincular, conectar, consultar) se prueba manualmente con un número secundario.

**Hipótesis a medir:** con credenciales ya guardadas, "conexión en frío + una consulta" tarda **≤ 3 s en p95** en una red doméstica.

## Diseño

```
prototypes/baileys-cli/
├── package.json            # @whiskeysockets/baileys fijado, tsx, libphonenumber-js, pino
├── tsconfig.json
├── .gitignore              # .auth/  results/  node_modules/
├── README.md               # uso y advertencias
└── src/
    ├── cli.ts              # parseo de comandos: link | check | bench | status | unlink
    ├── session.ts          # crea el socket con la config obligatoria; mide fases; espera 'open'
    ├── check.ts            # consulta con timeout → CheckResult
    ├── result.ts           # PURO: respuesta de onWhatsApp → CheckResult (+ jidMismatch)
    ├── phone.ts            # PURO: normalizar a E.164 sin '+', enmascarar
    ├── stats.ts            # PURO: min, p50, p95, max
    ├── output.ts           # tabla en consola + JSONL en results/
    └── *.test.ts           # node:test
```

| Módulo | Cambio |
|---|---|
| `prototypes/baileys-cli/` | Nuevo. No forma parte de la app Android; es herramienta de la Fase 0. |

Flujo de `check`:
```
t0 ─ makeWASocket(creds guardadas) ─ t_ws (socket abierto) ─ t_ready (connection: 'open')
   └─ por cada número: onWhatsApp(e164) con timeout 5 s ─ t_query ─ traducir ─ pausa 3–6 s
   └─ sock.end() (sin logout)
```

Configuración de la sesión (RF-08), en `session.ts`:
- `markOnlineOnConnect: false`
- `syncFullHistory: false` y `shouldSyncHistoryMessage: () => false`
- Sin llamar nunca a `readMessages` ni `sendPresenceUpdate`
- `messages.upsert` sin manejador que guarde nada; el logger de Baileys en nivel `warn`, con un serializador que enmascara números.
- `browser`: por defecto `Browsers.macOS('Chrome')`, el valor por defecto de Baileys (se ve como "Chrome (Mac OS)"). Con la identidad propia `['WhatsCheck', 'Chrome', '0.1.0']` el teléfono respondió *"No se pudo vincular el dispositivo"* (causa sin confirmar). `--browser` permite comparar `macos`, `ubuntu`, `windows` y `whatscheck`. Si solo funcionan las identidades predefinidas, se registra en `intentos-fallidos.md` y se revisa el requisito de "nombre de dispositivo honesto" de tech-stack §2.

## Decisiones

| Decisión | Alternativas consideradas | Motivo |
|---|---|---|
| TypeScript + `tsx` | JavaScript plano; TS compilado con `tsc` | Tipos de Baileys para no adivinar la API, sin paso de build |
| `@whiskeysockets/baileys@7.0.0-rc14` fijado | `6.7.24` (legacy); paquete `baileys` | Es la versión `latest` y la misma que usa con éxito el proyecto existente del autor. |
| `useMultiFileAuthState` en `.auth/` | Guardado propio cifrado | Suficiente para un prototipo local; el cifrado con Keystore es tema de Android |
| `libphonenumber-js` | Normalizar a mano | Misma lógica que usará la app (libphonenumber); evita errores con prefijos |
| `node:test` | Jest, Vitest | Sin dependencias extra; suficiente para funciones puras |
| Pausas y topes en lotes (RNF-01) | Consultar todo seguido | La cuenta de prueba se reutiliza en la prueba de baneos; no hay que quemarla antes |
| `onWhatsApp` → `undefined` se trata como `UNKNOWN` (`no_response`) | Tratarlo como `[]` | En Baileys 7, `onWhatsApp` devuelve `undefined` cuando la consulta USync no trae resultado. Tratarlo como "no tiene" violaría *fallar en silencio*. |
| Nuevo motivo `not_connected` | Reusar `session_closed` | Distingue "no se pudo conectar" (red, timeout) de "WhatsApp cerró la sesión" (401/403), que exige volver a vincular |
| Una sesión cuenta como vinculada solo con `registered` **y** `account`; `link` borra credenciales incompletas | Confiar solo en `registered` | Un intento de vinculación sin terminar dejaba credenciales que provocaban 401 en cada reintento ([IF-008](../product/intentos-fallidos.md#if-008--reintentar-la-vinculación-con-credenciales-de-un-intento-fallido)) |
| Un 401 borra las credenciales locales automáticamente | Obligar a usar `unlink` | Tras cerrar la sesión desde el teléfono, las credenciales ya no sirven; exigir `unlink` antes de `link` confundía (reportado por el autor). Con 403 se conservan para diagnosticar. La app Android debe hacer lo mismo |
| `await sock.end()` antes de salir | Esperar `sock.ws.once('close')` | Ver [IF-010](../product/intentos-fallidos.md#if-010--salir-sin-esperar-a-que-baileys-cierre-el-websocket) |
| Se acepta que libphonenumber quite el `0` tras el código de país | Cambiar el orden de interpretación | Solo afecta a números imposibles; ver [IF-007](../product/intentos-fallidos.md#if-007--usar-51000000001-como-número-inexistente-tal-cual) |



| Riesgo | Mitigación |
|---|---|
| `7.0.0-rc14` tiene errores por ser *release candidate* | Probar con `6.7.24` si falla la vinculación o la consulta; registrar en `intentos-fallidos.md` |
| El código de emparejamiento no funciona con un nombre de dispositivo propio | Usar un perfil predefinido de `Browsers` (ver pregunta abierta) |
| Restricción de la cuenta secundaria durante las mediciones | Pausas y topes (RNF-01); detener todo ante el primer aviso (RF-09) |
| Al conectar en frío llega una ráfaga de mensajes pendientes que retrasa la sesión | Medirlo: es justo lo que el prototipo debe revelar. Anotar si `t_ready` crece con el tiempo desconectado |
| Sin sincronización inicial, Baileys 7 no tiene las equivalencias número ↔ LID y advierte de "inestabilidad y errores de sesión" | Se mantiene desactivada (RF-08) porque `onWhatsApp` no depende de ellas; se vigilan errores en las pruebas. Ver pregunta abierta en spec.md |
| La latencia de escritorio no representa la de Android con datos móviles | Anotar el tipo de red; la medición en Android es parte del ítem de comparación de motores |

## Cambios a documentos base

- **Aprobado y aplicado (2026-09-23):** `tech-stack.md` §2, requisito *"Nombre de dispositivo visible y honesto (p. ej. 'WhatsCheck')"* → *"Con código de emparejamiento, WhatsApp no muestra el nombre que envía el cliente (se ve p. ej. 'Google Chrome (Mac OS)'). El onboarding debe decirle al usuario con qué nombre aparecerá. Mostrar un nombre propio queda como investigación de la Fase 1 (spec de vinculación)."* Motivo: [IF-009](../product/intentos-fallidos.md#if-009--vincular-con-un-sistema-operativo-inventado-en-browser).
- `tech-stack.md` §6 *Calidad y entrega*: agregar que los prototipos de la Fase 0 viven en `prototypes/` y no forman parte de la app. **Aprobado y aplicado (2026-09-23).**

